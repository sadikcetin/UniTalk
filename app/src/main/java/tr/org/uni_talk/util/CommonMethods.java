package tr.org.uni_talk.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import tr.org.uni_talk.account.UniTalkAccountManager;
import tr.org.uni_talk.app.UniTalkApplication;


public class CommonMethods {


    public static String getBareJID(String name) {
        if (name.contains("/"))
            return name.split("/")[0].trim();
        return name;
    }

    public static String getSecondBareJID(String name) {
        if (name.contains("/"))
            return name.split("/")[1].trim();
        return name;
    }

    public static String getNumber(String jid) {
        if (jid.contains("@"))
            return jid.split("@")[0].trim();
        return jid;
    }

    public static String generatePassword(String digestAlgorithm, String userName) throws NoSuchAlgorithmException {
        String password = null;
        // Create MD5 Hash
        MessageDigest digest = java.security.MessageDigest.getInstance(digestAlgorithm);
        digest.update(userName.getBytes());
        digest.update(String.valueOf(System.currentTimeMillis()).getBytes());
        byte messageDigest[] = digest.digest();

        // Create Hex String
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < messageDigest.length; i++)
            hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
        password = hexString.toString();
        return password;
    }

    public static String generatePassword(String userName) throws NoSuchAlgorithmException {
        return generatePassword("MD5", userName);
    }

    public static List<String> getContactNumbers(Context context) {

        Cursor phones = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);

        List<String> phoneList = null;
        if (phones != null) {

            phoneList = new ArrayList<>(phones.getCount());
            while (phones.moveToNext()) {
                phoneList.add(phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
            }
        }
        return phoneList;
    }

    public static String getNameFromPhoneNumber(Context context, String number) {

        String displayName = null;
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

        String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME};

        Cursor contacts = context.getContentResolver().query(uri, projection, null, null, null);
        if (contacts != null && contacts.getCount() > 0) {
            contacts.moveToFirst();
            displayName = contacts.getString(contacts.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
        }
        contacts.close();
        return displayName;
    }

    public static String getMemberNameString (String receiverString){
        List<String> memberNumbers = Arrays.asList(receiverString.split(","));
        String result = "";
        String number = CommonMethods.getNumber(UniTalkAccountManager.getInstance().getAccount().getUserName());
        for (String s:memberNumbers) {
            String name = CommonMethods.getNameFromPhoneNumber(UniTalkApplication.getInstance().getApplicationContext(), s);
            if (s.equals(number))
                result += ",Me";
            else if (name == null)
                result += "," + s;
            else
                result += "," + name;
        }
        return result.substring(1);
    }

    public static String readDataFromXml(String xml , String tagName) {
        String data = null;
        DocumentBuilder builder = null;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource src = new InputSource();
            src.setCharacterStream(new StringReader(xml));

            Document doc = builder.parse(src);
            data = doc.getElementsByTagName(tagName).item(0).getTextContent();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (NullPointerException ne) {
            ne.printStackTrace();
        } catch (SAXException | IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    public static String getStringBetweenTwoChracters(String incomingText,String ch1 ,String ch2){
        String s = incomingText;
        s = s.substring(s.indexOf(ch1) + 1);
        s = s.substring(0, s.indexOf(ch2));
        return  s;
    }

}
