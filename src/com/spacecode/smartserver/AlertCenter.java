package com.spacecode.smartserver;

import com.spacecode.sdk.device.event.AccessControlEventHandler;
import com.spacecode.sdk.device.event.DeviceEventHandler;
import com.spacecode.sdk.device.event.DoorEventHandler;
import com.spacecode.sdk.user.AccessType;
import com.spacecode.sdk.user.GrantedUser;
import com.spacecode.smartserver.database.DatabaseHandler;
import com.spacecode.smartserver.database.entity.SmtpServerEntity;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
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

        DeviceHandler.getDevice().addListener(new AlertEventHandler());
        sendEmail();
        return true;
    }

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

    private static void sendEmail()
    {
        try
        {
            InternetAddress[] toList = InternetAddress.parse(_smtpServerConfiguration.getUsername());
            InternetAddress[] ccList = InternetAddress.parse(_smtpServerConfiguration.getUsername());
            InternetAddress[] bccList = InternetAddress.parse("vince.g.135@gmail.com");

            MimeMessage message = new MimeMessage(_mailSession);
            message.setSubject("Sample Subject");
            message.setFrom(new InternetAddress(_smtpServerConfiguration.getUsername()));
            message.setRecipients(Message.RecipientType.TO, toList);
            message.addRecipients(Message.RecipientType.CC, ccList);
            message.addRecipients(Message.RecipientType.BCC, bccList);
            message.setContent("Sample content.", "text/html");

            Transport.send(message);
        } catch (MessagingException me)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Exception occurred while sending an alert email.", me);
        }
    }

    private static class AlertEventHandler implements DeviceEventHandler,
            DoorEventHandler,
            AccessControlEventHandler
    {
        @Override
        public void deviceDisconnected()
        {
            // TODO: Raise alert
        }

        @Override
        public void doorOpenDelay()
        {
            // TODO: Raise alert
        }

        @Override
        public void authenticationSuccess(GrantedUser grantedUser, AccessType accessType, boolean isMaster)
        {
            // TODO: Raise alert if "thief finger"
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
