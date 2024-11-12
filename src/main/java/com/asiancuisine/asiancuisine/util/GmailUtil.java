package com.asiancuisine.asiancuisine.util;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.GmailScopes;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.auth.oauth2.ServiceAccountCredentials;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Collections;
import java.util.stream.Collectors;

public class GmailUtil {

    public static void sendVerificationEmail(String toEmailAddress, int verificationCode) throws Exception {
        // load template
        String htmlTemplate = loadEmailTemplate("gmail/email_template.html");

        // replace {verification-code} to your real verification code
        String verificationCodeStr = formatNumberWithSpaces(verificationCode);
        String htmlContent = htmlTemplate.replace("{verification-code}", verificationCodeStr);

        // send email
        sendEmail(toEmailAddress, "[ACFJ] Reset Password Verification Code", htmlContent);
    }

    public static void sendEmail(String to, String subject, String bodyHtml) throws Exception {
        Gmail service = GmailService.getGmailService();
        MimeMessage email = createEmail(to, "asiancuisineforjobless@gmail.com", subject, bodyHtml);
        Message message = createMessageWithEmail(email);
        service.users().messages().send("me", message).execute();
    }

    public static String loadEmailTemplate(String templatePath) throws IOException {
        InputStream inputStream = GmailUtil.class.getClassLoader().getResourceAsStream(templatePath);

        if (inputStream == null) {
            throw new IOException("Resource not found: " + templatePath);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private static MimeMessage createEmail(String to, String from, String subject, String bodyText) throws MessagingException {
        javax.mail.Session session = javax.mail.Session.getDefaultInstance(new java.util.Properties(), null);
        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(from));
        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setContent(bodyText, "text/html; charset=utf-8");
        return email;
    }

    private static Message createMessageWithEmail(MimeMessage emailContent) throws MessagingException, IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.getUrlEncoder().encodeToString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    private static String formatNumberWithSpaces(int number) {
        String numberStr = String.valueOf(number);
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < numberStr.length(); i++) {
            formatted.append(numberStr.charAt(i));
            if (i < numberStr.length() - 1) {
                formatted.append(" ");
            }
        }
        return formatted.toString();
    }
}

class GmailService {
    private static final String APPLICATION_NAME = "asfj-email-backend";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String CREDENTIALS_FILE_PATH = "/gmail/token.json";
    private static final String TOKENS_DIRECTORY_PATH = System.getProperty("user.home") + "/.credentials/asfj-email-backend/tokens";

    public static Gmail getGmailService() throws GeneralSecurityException, IOException {
        InputStream serviceAccountTokenStream = GmailService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (serviceAccountTokenStream == null) {
            throw new IOException("Could not find token.json");
        }

        Credential credential = getCredentials();
        return new Gmail.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private static Credential getCredentials() throws IOException, GeneralSecurityException {
        InputStream credentialsStream = GmailService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (credentialsStream == null) {
            throw new IOException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(credentialsStream));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, clientSecrets, Collections.singleton(GmailScopes.GMAIL_SEND))
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }
}
