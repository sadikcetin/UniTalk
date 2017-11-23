package tr.org.uni_talk.mail;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Build;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import tr.org.uni_talk.app.UniTalkApplication;
import tr.org.uni_talk.ui.toolbar.FeedBackActivity;


public class MailSender {

    String mailText = null, imagePath = null, userName = null;
    int limit = 5;

    public MailSender(String mailText, String imagePath, String userName) {
        this.mailText = mailText;
        this.imagePath = imagePath;
        this.userName = userName;
    }

    public void sendMail() {

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);

        final String username = ""; // feedback mail address
        final String password = ""; // mail password


            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "");
            props.put("mail.smtp.port", "");

            Session session = Session.getInstance(props,
                    new javax.mail.Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(username, password);
                        }
                    });

            try {
                final Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress("")); // sender mail address
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse("")); // receiver mail address
                message.setSubject("[#] UniTalk BUG Report");
                Log.e("Mail Builder", buildMessageBody());
                message.setText(buildMessageBody());
                addPhotoToMail(message);
                Transport.send(message);


            } catch (MessagingException e) {
                Log.e("Feedback Mail", e.getMessage());


            }

    }

    public boolean lessThenLimitMb(String imagePath) {
        long size;
        File file = new File(imagePath);
        size = file.length() / (1024 * 1024);
        return (size <= limit) ? true : false;
    }

    public void addPhotoToMail(Message message) throws MessagingException {
        if (imagePath != null) {

            if (lessThenLimitMb(imagePath)) {
                MimeBodyPart attachmentBodyPart = new MimeBodyPart();
                MimeBodyPart messageBodyBodyPart = new MimeBodyPart();
                messageBodyBodyPart.setText(buildMessageBody());
                Multipart multipart = new MimeMultipart();
                String fileName = "Screenshot";
                DataSource source = new FileDataSource(imagePath);
                attachmentBodyPart.setDataHandler(new DataHandler(source));
                attachmentBodyPart.setFileName(fileName);
                multipart.addBodyPart(attachmentBodyPart);
                multipart.addBodyPart(messageBodyBodyPart);
                message.setContent(multipart);
            } else {
                Toast.makeText(UniTalkApplication.getInstance().getApplicationContext(), "Image can not be more than 5 MB !", Toast.LENGTH_SHORT).show();
            }

        }
    }

    public String buildMessageBody() {
        StringBuilder builder = new StringBuilder();
        builder.append("Username      : " + userName + "\n");
        builder.append("--------------------------------\n");
        builder.append("Message Body  :" + mailText + "\n");
        builder.append("--------------------------------\n");
        builder.append("Phone Model   :" + Build.MODEL + "\n");
        builder.append("Phone Release :" + Build.VERSION.RELEASE + "\n");
        builder.append("Phone SDK     :" + Build.VERSION.SDK_INT);

        return builder.toString();
    }
}
