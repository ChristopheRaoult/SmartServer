package com.spacecode.smartserver;

import com.spacecode.sdk.device.event.AccessControlEventHandler;
import com.spacecode.sdk.device.event.DeviceEventHandler;
import com.spacecode.sdk.device.event.DoorEventHandler;
import com.spacecode.sdk.user.AccessType;
import com.spacecode.sdk.user.FingerIndex;
import com.spacecode.sdk.user.GrantedUser;
import com.spacecode.smartserver.database.DatabaseHandler;
import com.spacecode.smartserver.database.entity.*;
import com.spacecode.smartserver.database.repository.AlertHistoryRepository;
import com.spacecode.smartserver.database.repository.AlertRepository;
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
public class AlertCenter
{
    private static Session _mailSession;
    private static SmtpServerEntity _smtpServerConfiguration;

    /** AlertRepository */
    private static Repository<AlertEntity> _alertRepository;

    /** AlertHistoryRepository */
    private static Repository<AlertHistoryEntity> _alertHistoryRepository;

    /** list of all alert type entities, in order to keep them at hand */
    private static Map<String, AlertTypeEntity> _alertTypeToEntities;

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

        if(!initializeSmtpServer())
        {
            SmartLogger.getLogger().severe("No SMTP server is set. Can't start AlertCenter.");
            return false;
        }

        if(!initializeRepositories())
        {
            SmartLogger.getLogger().severe("Could not initialize Repositories. Can't start AlertCenter.");
            return false;
        }

        if(!initializeAlertTypeEntities())
        {
            SmartLogger.getLogger().severe("Could not get alert types. Can't start AlertCenter.");
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
     * Fill the list of known alert types.
     * @return  true if succeeded, false otherwise.
     */
    private static boolean initializeAlertTypeEntities()
    {
        Repository<AlertTypeEntity> atRepo = DatabaseHandler.getRepository(AlertTypeEntity.class);
        List<AlertTypeEntity> atList = atRepo.getAll();

        if(atList == null || atList.size() == 0)
        {
            return false;
        }

        _alertTypeToEntities = new HashMap<>();

        for(AlertTypeEntity ate : atList)
        {
            _alertTypeToEntities.put(ate.getType(), ate);
        }

        return true;
    }

    /**
     * Get the Alert Repository to keep it at hand.
     * @return  true if succeeded, false otherwise.
     */
    private static boolean initializeRepositories()
    {
        _alertRepository = DatabaseHandler.getRepository(AlertEntity.class);
        _alertHistoryRepository = DatabaseHandler.getRepository(AlertHistoryEntity.class);

        return  _alertRepository != null &&
                _alertRepository instanceof AlertRepository &&
                _alertHistoryRepository != null &&
                _alertHistoryRepository instanceof AlertHistoryRepository;
    }

    /**
     * Use SMTP server information (from database) to send an email according to alert settings.
     * @param alertEntity Alert containing the emailing information.
     */
    private static void sendEmail(AlertEntity alertEntity)
    {
        try
        {
            InternetAddress[] toList = InternetAddress.parse(alertEntity.getToList());
            InternetAddress[] ccList = InternetAddress.parse(alertEntity.getCcList());
            InternetAddress[] bccList = InternetAddress.parse(alertEntity.getBccList());

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
     * @param ate Desired AlertType.
     */
    private static void raiseAlerts(AlertTypeEntity ate)
    {
        List<AlertEntity> thiefFingerAlerts = _alertRepository.getEntitiesBy(AlertEntity.ALERT_TYPE_ID, ate.getId());
        Iterator<AlertEntity> it = thiefFingerAlerts.iterator();

        while(it.hasNext())
        {
            if(!it.next().isEnabled())
            {
                it.remove();
            }
        }

        if(thiefFingerAlerts.size() == 0)
        {
            return;
        }

        List<AlertHistoryEntity> alertHistoryEntities = new ArrayList<>();

        for(AlertEntity ae : thiefFingerAlerts)
        {
            alertHistoryEntities.add(new AlertHistoryEntity(ae));
            sendEmail(ae);
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
            AccessControlEventHandler
    {
        @Override
        public void deviceDisconnected()
        {
            AlertTypeEntity ate = _alertTypeToEntities.get(AlertTypeEntity.DEVICE_DISCONNECTED);

            if(ate == null)
            {
                return;
            }

            raiseAlerts(ate);
        }

        @Override
        public void doorOpenDelay()
        {
            AlertTypeEntity ate = _alertTypeToEntities.get(AlertTypeEntity.DOOR_DELAY);

            if(ate == null)
            {
                return;
            }

            raiseAlerts(ate);
        }

        @Override
        public void authenticationSuccess(GrantedUser grantedUser, AccessType accessType, boolean isMaster)
        {
            Repository<GrantedUserEntity> userRepo = DatabaseHandler.getRepository(GrantedUserEntity.class);

            GrantedUserEntity gue = userRepo.getEntityBy(GrantedUserEntity.USERNAME, grantedUser.getUsername());

            if(gue == null)
            {
                return;
            }

            FingerIndex index = DeviceHandler.getDevice().getUsersService().getLastFingerIndex(isMaster);
            AlertTypeEntity ate = _alertTypeToEntities.get(AlertTypeEntity.THIEF_FINGER);

            if(index == null || index.getIndex() != gue.getThiefFingerIndex() || ate == null)
            {
                return;
            }

            raiseAlerts(ate);
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
