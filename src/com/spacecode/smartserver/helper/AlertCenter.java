package com.spacecode.smartserver.helper;

import com.spacecode.sdk.device.event.AccessControlEventHandler;
import com.spacecode.sdk.device.event.DeviceEventHandler;
import com.spacecode.sdk.device.event.DoorEventHandler;
import com.spacecode.sdk.device.event.TemperatureEventHandler;
import com.spacecode.sdk.device.module.TemperatureProbe;
import com.spacecode.sdk.network.alert.AlertType;
import com.spacecode.sdk.network.communication.EventCode;
import com.spacecode.sdk.user.User;
import com.spacecode.sdk.user.data.AccessType;
import com.spacecode.sdk.user.data.FingerIndex;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DatabaseHandler;
import com.spacecode.smartserver.database.entity.*;
import com.spacecode.smartserver.database.repository.AlertHistoryRepository;
import com.spacecode.smartserver.database.repository.AlertRepository;
import com.spacecode.smartserver.database.repository.AlertTypeRepository;
import com.spacecode.smartserver.database.repository.Repository;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.*;
import java.util.logging.Level;

/**
 * Handle Alerts raising/reporting and Emails sending (if any SMTP server is set).
 *
 * Has to be initialized to subscribe to "alert-compliant" events.
 */
public final class AlertCenter
{
    private static Session _mailSession;
    private static SmtpServerEntity _smtpServerConfiguration;
    private static boolean _isSmtpServerSet;
    private static String _lastAuthenticatedUsername;

    private static AlertRepository _alertRepository;
    private static Repository<AlertHistoryEntity> _alertHistoryRepository;
    private static AlertTypeRepository _alertTypeRepository;

    /** Must not be instantiated */
    private AlertCenter()
    {
    }

    /**
     * Initialize subscriptions to device events. Will allow handling specific alerts, if required.
     * @return  True if operation is successful. False otherwise (device null / not instantiated).
     */
    public static boolean initialize()
    {
        if(DeviceHandler.getDevice() == null)
        {
            return false;
        }

        _isSmtpServerSet = initializeSmtpServer();

        if(!_isSmtpServerSet)
        {
            SmartLogger.getLogger().severe("No SMTP server is set. AlertCenter won't send any email.");
        }

        if(!initializeRepositories())
        {
            SmartLogger.getLogger().severe("Could not initialize Repositories. Can't start AlertCenter.");
            return false;
        }

        DeviceHandler.getDevice().addListener(new AlertEventHandler());
        return true;
    }

    /**
     * Get SMTP server information from DB. Initialize a "Session" (javax.mail) instance used to send emails.
     * @return  true if initialization succeeded, false otherwise (no SMTP server set in DB).
     */
    private static boolean initializeSmtpServer()
    {
        final SmtpServerEntity sse = DatabaseHandler.getSmtpServerConfiguration();

        if(sse == null)
        {
            return false;
        }

        _smtpServerConfiguration = sse;

        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.host", sse.getAddress());
        props.put("mail.smtp.auth", "true");
        // enable/disable logging of JavaMail library
        props.put("mail.debug", "false");
        props.put("mail.smtp.port", sse.getPort());
        props.put("mail.smtp.socketFactory.port", sse.getPort());
        props.put("mail.smtp.starttls.enable", sse.isSslEnabled());

        _mailSession = Session.getInstance(props,
            new Authenticator()
            {
                protected PasswordAuthentication getPasswordAuthentication()
                {
                    return new PasswordAuthentication(sse.getUsername(), sse.getPassword());
                }
            });

        return true;
    }

    /**
     * Get the Alert Repository to keep it at hand.
     * @return  true if succeeded, false otherwise.
     */
    private static boolean initializeRepositories()
    {
        _alertRepository = (AlertRepository) DatabaseHandler.getRepository(AlertEntity.class);
        _alertHistoryRepository = DatabaseHandler.getRepository(AlertHistoryEntity.class);
        _alertTypeRepository = (AlertTypeRepository) DatabaseHandler.getRepository(AlertTypeEntity.class);

        return  _alertRepository != null &&
                _alertRepository instanceof AlertRepository &&
                _alertHistoryRepository != null &&
                _alertHistoryRepository instanceof AlertHistoryRepository &&
                _alertTypeRepository != null;
    }

    /**
     * Use SMTP server information (from database) to send an email according to alert settings.
     * @param alertEntity Alert containing the emailing information.
     */
    private static void sendEmail(AlertEntity alertEntity)
    {
        if(!_isSmtpServerSet)
        {
            return;
        }

        try
        {
            String recipientsTo = alertEntity.getToList();
            String recipientsCc = alertEntity.getCcList();
            String recipientsBcc = alertEntity.getBccList();

            InternetAddress[] toList = InternetAddress.parse(recipientsTo == null ? "" : recipientsTo);
            InternetAddress[] ccList = InternetAddress.parse(recipientsCc == null ? "" : recipientsCc);
            InternetAddress[] bccList = InternetAddress.parse(recipientsBcc == null ? "" : recipientsBcc);

            MimeMessage message = new MimeMessage(_mailSession);
            message.setSubject(alertEntity.getEmailSubject());
            message.setFrom(new InternetAddress(_smtpServerConfiguration.getUsername()));
            message.setRecipients(Message.RecipientType.TO, toList);
            message.addRecipients(Message.RecipientType.CC, ccList);
            message.addRecipients(Message.RecipientType.BCC, bccList);
            message.setContent(alertEntity.getEmailContent(), "text/html");

            Transport.send(message);
        } catch (MessagingException me)
        {
            SmartLogger.getLogger().log(Level.SEVERE,
                    "Exception occurred while sending an alert email. Id: "+alertEntity.getId(), me);
        }
    }

    /**
     * Record a new AlertHistory and send an email for each alert in the list.
     *
     * @param matchingAlerts    Enabled alerts to be recorded and sent.
     * @param extraData         Additional data provided with the alert report (Username, Temperature...).
     */
    private static void recordAndSend(Collection<AlertEntity> matchingAlerts, String extraData)
    {
        if(matchingAlerts == null || matchingAlerts.isEmpty())
        {
            return;
        }

        List<AlertHistoryEntity> alertHistoryEntities = new ArrayList<>();

        for(AlertEntity ae : matchingAlerts)
        {
            alertHistoryEntities.add(new AlertHistoryEntity(ae, extraData));
            sendEmail(ae);
            SmartLogger.getLogger().info("Raising an Alert (id: "+ae.getId()+")!");
        }

        if(!_alertHistoryRepository.insert(alertHistoryEntities))
        {
            SmartLogger.getLogger().severe("Unable to insert AlertHistory entities.");
        }
    }

    /**
     * Listener subscribing to appropriate Device events in order to raise alerts.
     */
    private static class AlertEventHandler implements DeviceEventHandler,
            DoorEventHandler,
            AccessControlEventHandler,
            TemperatureEventHandler
    {
        @Override
        public void deviceDisconnected()
        {
            AlertTypeEntity alertTypeDisconnected = _alertTypeRepository.fromAlertType(AlertType.DEVICE_DISCONNECTED);

            if(alertTypeDisconnected == null)
            {
                return;
            }

            List<AlertEntity> matchingAlerts =
                    _alertRepository.getEnabledAlerts(alertTypeDisconnected, DatabaseHandler.getDeviceConfiguration());

            // notify alerts (event)
            List<Entity> notifiableAlerts = new ArrayList<>();
            notifiableAlerts.addAll(matchingAlerts);
            notifyAlertEvent(notifiableAlerts, "");

            // save history in DB and send email
            recordAndSend(matchingAlerts, "");
        }

        @Override
        public void doorOpenDelay()
        {
            AlertTypeEntity alertTypeDoorDelay = _alertTypeRepository.fromAlertType(AlertType.DOOR_OPEN_DELAY);

            if(alertTypeDoorDelay == null)
            {
                return;
            }

            List<AlertEntity> matchingAlerts =
                    _alertRepository.getEnabledAlerts(alertTypeDoorDelay, DatabaseHandler.getDeviceConfiguration());

            // notify alerts (event)
            List<Entity> notifiableAlerts = new ArrayList<>();
            notifiableAlerts.addAll(matchingAlerts);
            notifyAlertEvent(notifiableAlerts, _lastAuthenticatedUsername);

            // save history in DB and send email
            recordAndSend(matchingAlerts, _lastAuthenticatedUsername);
        }

        @Override
        public void authenticationSuccess(final User grantedUser,
                                          AccessType accessType, final boolean isMaster)
        {
            _lastAuthenticatedUsername = grantedUser.getUsername();

            // we're only interested in fingerprint authentications for "thief finger" alert.
            if(accessType != AccessType.FINGERPRINT)
            {
                return;
            }

            Repository<UserEntity> userRepo = DatabaseHandler.getRepository(UserEntity.class);
            UserEntity gue = userRepo.getEntityBy(UserEntity.USERNAME, grantedUser.getUsername());

            // no matching user, or user has no "finger thief" index set.
            if(gue == null || gue.getThiefFingerIndex() == null)
            {
                return;
            }

            // get the FingerIndex value of the last fingerprint scanned
            FingerIndex index = DeviceHandler.getDevice().getUsersService().getLastVerifiedFingerIndex(isMaster);
            AlertTypeEntity alertTypeThiefFinger = _alertTypeRepository.fromAlertType(AlertType.THIEF_FINGER);

            if(index == null || alertTypeThiefFinger == null || index.getIndex() != gue.getThiefFingerIndex())
            {
                return;
            }

            List<AlertEntity> matchingAlerts =
                    _alertRepository.getEnabledAlerts(alertTypeThiefFinger, DatabaseHandler.getDeviceConfiguration());

            // notify alerts (event)
            List<Entity> notifiableAlerts = new ArrayList<>();
            notifiableAlerts.addAll(matchingAlerts);
            notifyAlertEvent(notifiableAlerts, _lastAuthenticatedUsername);

            // save history in DB and send email
            recordAndSend(matchingAlerts, _lastAuthenticatedUsername);
        }

        @Override
        public void temperatureMeasure(double value)
        {
            if(value == TemperatureProbe.ERROR_VALUE)
            {
                return;
            }

            AlertTypeEntity alertTypeTemperature = _alertTypeRepository.fromAlertType(AlertType.TEMPERATURE);

            if(alertTypeTemperature == null)
            {
                return;
            }

            // get enabled Temperature Alerts
            List<AlertEntity> alerts = _alertRepository.getEnabledAlerts(alertTypeTemperature,
                    DatabaseHandler.getDeviceConfiguration());

            if(alerts.isEmpty())
            {
                return;
            }

            // next, take their Ids
            List<Integer> alertIds = new ArrayList<>();

            for(AlertEntity ae : alerts)
            {
                alertIds.add(ae.getId());
            }

            // get the attached AlertTemperature entities
            Repository<AlertTemperatureEntity> atRepo = DatabaseHandler.getRepository(AlertTemperatureEntity.class);
            List<AlertTemperatureEntity> atList = atRepo.getAllWhereFieldIn(AlertTemperatureEntity.ALERT_ID, alertIds);

            Map<Entity, AlertEntity> matchingAlerts = new HashMap<>();

            for(AlertTemperatureEntity at : atList)
            {
                // if threshold triggered: need to be raised
                if( value > at.getTemperatureMax() ||
                    value < at.getTemperatureMin())
                {
                    matchingAlerts.put(at, at.getAlert());
                }
            }

            // if temperature alert needs to be raised
            if(matchingAlerts.isEmpty())
            {
                return;
            }

            String extraData = String.valueOf(value);

            // notify alerts (event)
            notifyAlertEvent(matchingAlerts.keySet(), extraData);

            // Now we have all enabled alerts with threshold triggered (temperature too low or too high)
            // 1 Alert (entity) for 1 AlertTemperature (entity): no check for redundancy or 1-n relationship issues
            recordAndSend(matchingAlerts.values(), extraData);
        }

        /**
         * Send an Alert event to all listening clients for each alert in the list.
         *
         * @param alertEntities Alert to be notified.
         */
        private void notifyAlertEvent(Collection<Entity> alertEntities, String additionalData)
        {
            for(Entity alertEntity : alertEntities)
            {
                if(alertEntity instanceof AlertEntity)
                {
                    SmartServer.sendAllClients(EventCode.ALERT,
                            AlertEntity.toAlert((AlertEntity) alertEntity).serialize(), additionalData);
                }

                else if(alertEntity instanceof AlertTemperatureEntity)
                {
                    SmartServer.sendAllClients(EventCode.ALERT,
                            AlertEntity.toAlert((AlertTemperatureEntity) alertEntity).serialize(), additionalData);
                }
            }
        }

        @Override
        public void scanCancelledByDoor()
        {
            // not required
        }

        @Override
        public void doorOpened()
        {
            // not required
        }

        @Override
        public void doorClosed()
        {
            // not required
        }

        @Override
        public void authenticationFailure(User grantedUser, AccessType accessType, boolean isMaster)
        {
            // not required
        }
    }
}
