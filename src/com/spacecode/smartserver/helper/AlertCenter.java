package com.spacecode.smartserver.helper;

import com.spacecode.sdk.device.event.AccessControlEventHandler;
import com.spacecode.sdk.device.event.DeviceEventHandler;
import com.spacecode.sdk.device.event.DoorEventHandler;
import com.spacecode.sdk.device.event.TemperatureEventHandler;
import com.spacecode.sdk.network.alert.AlertType;
import com.spacecode.sdk.user.AccessType;
import com.spacecode.sdk.user.FingerIndex;
import com.spacecode.sdk.user.GrantedUser;
import com.spacecode.smartserver.database.DatabaseHandler;
import com.spacecode.smartserver.database.entity.*;
import com.spacecode.smartserver.database.repository.AlertHistoryRepository;
import com.spacecode.smartserver.database.repository.AlertRepository;
import com.spacecode.smartserver.database.repository.AlertTypeRepository;
import com.spacecode.smartserver.database.repository.Repository;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Handle Alerts raising/reporting and Emails sending (if any SMTP server is set).
 *
 * Has to be initialized to subscribe to "alert-compliant" events.
 *
 * TODO: (not done yet) Query only alerts with DeviceEntity = DatabaseHandler.getDeviceConfiguration()
 */
public final class AlertCenter
{
    private static Session _mailSession;
    private static SmtpServerEntity _smtpServerConfiguration;
    private static boolean _isSmtpServerSet;

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
        props.put("mail.debug", "true");
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
                _alertTypeRepository != null &&
                _alertTypeRepository instanceof AlertTypeRepository;
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
            SmartLogger.getLogger().log(Level.SEVERE, "Exception occurred while sending an alert email. Id: "+alertEntity.getId(), me);
        }
    }

    /**
     * Raise all enabled alerts for the given alert type.
     *
     * @param ate Desired AlertType.
     */
    private static void raiseAlerts(AlertTypeEntity ate)
    {
        List<AlertEntity> matchingAlerts = _alertRepository.getEnabledAlerts(ate);

        if(matchingAlerts.isEmpty())
        {
            return;
        }

        List<AlertHistoryEntity> alertHistoryEntities = new ArrayList<>();

        for(AlertEntity ae : matchingAlerts)
        {
            alertHistoryEntities.add(new AlertHistoryEntity(ae));
            sendEmail(ae);
            SmartLogger.getLogger().info("Raising an Alert (id: "+ae.getId()+")!");
        }

        if(!_alertHistoryRepository.insert(alertHistoryEntities))
        {
            SmartLogger.getLogger().severe("Unable to insert AlertHistory entities.");
        }
    }

    /**
     * Raise all alerts passed in parameter.
     *
     * @param matchingAlerts Enabled alerts to be raised.
     */
    private static void raiseAlerts(List<AlertEntity> matchingAlerts)
    {
        if(matchingAlerts == null || matchingAlerts.isEmpty())
        {
            return;
        }

        List<AlertHistoryEntity> alertHistoryEntities = new ArrayList<>();

        for(AlertEntity ae : matchingAlerts)
        {
            alertHistoryEntities.add(new AlertHistoryEntity(ae));
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
            AlertTypeEntity ate = _alertTypeRepository.fromAlertType(AlertType.DEVICE_DISCONNECTED);

            if(ate == null)
            {
                return;
            }

            raiseAlerts(ate);
        }

        @Override
        public void doorOpenDelay()
        {
            AlertTypeEntity ate = _alertTypeRepository.fromAlertType(AlertType.DOOR_OPEN_DELAY);

            if(ate == null)
            {
                return;
            }

            raiseAlerts(ate);
        }

        @Override
        public void authenticationSuccess(final GrantedUser grantedUser, AccessType accessType, final boolean isMaster)
        {
            // we're only interested in fingerprint authentications for "thief finger" alert.
            if(accessType != AccessType.FINGERPRINT)
            {
                return;
            }

            Repository<GrantedUserEntity> userRepo = DatabaseHandler.getRepository(GrantedUserEntity.class);
            GrantedUserEntity gue = userRepo.getEntityBy(GrantedUserEntity.USERNAME, grantedUser.getUsername());

            // no matching user, or user has no "finger thief" index set.
            if(gue == null || gue.getThiefFingerIndex() == null)
            {
                return;
            }

            FingerIndex index = DeviceHandler.getDevice().getUsersService().getLastFingerIndex(isMaster);
            AlertTypeEntity ate = _alertTypeRepository.fromAlertType(AlertType.THIEF_FINGER);

            if(index == null || ate == null || index.getIndex() != gue.getThiefFingerIndex())
            {
                return;
            }

            raiseAlerts(ate);
        }

        @Override
        public void temperatureMeasure(double value)
        {
            AlertTypeEntity ate = _alertTypeRepository.fromAlertType(AlertType.TEMPERATURE);

            if(ate == null)
            {
                return;
            }

            // get enabled Temperature Alerts.
            List<AlertEntity> alerts = _alertRepository.getEnabledAlerts(ate);

            if(alerts.isEmpty())
            {
                return;
            }

            // next, take their Ids.
            List<Integer> alertIds = new ArrayList<>();

            for(AlertEntity ae : alerts)
            {
                alertIds.add(ae.getId());
            }

            // get the AlertTemperature entities.
            Repository<AlertTemperatureEntity> atRepo = DatabaseHandler.getRepository(AlertTemperatureEntity.class);
            List<AlertTemperatureEntity> atList = atRepo.getAllWhereFieldIn(AlertTemperatureEntity.ALERT_ID, alertIds);

            List<AlertEntity> matchingAlerts = new ArrayList<>();

            for(AlertTemperatureEntity at : atList)
            {
                // need to be raised: threshold triggered
                if( value > at.getTemperatureMax() ||
                    value < at.getTemperatureMin())
                {
                    matchingAlerts.add(at.getAlert());
                }
            }

            // if temperature alert needs to be raised.
            if(matchingAlerts.isEmpty())
            {
                return;
            }

            // Now we have all enabled alerts with threshold triggered (temperature too low or too high)
            // There is only 1 Alert for 1 AlertTemperature so we don't check for double, triple, redundancy, etc.
            raiseAlerts(matchingAlerts);
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
        public void authenticationFailure(GrantedUser grantedUser, AccessType accessType, boolean isMaster)
        {
            // not required
        }
    }
}
