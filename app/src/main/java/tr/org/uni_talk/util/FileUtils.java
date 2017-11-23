package tr.org.uni_talk.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import tr.org.uni_talk.pojo.User;

/**
 * Created by sadik on 8/11/16.
 */
public class FileUtils {

    public static void write(File registrationFile, String userName, String password) {
        FileWriter writer = null;
        try {
            if (!registrationFile.exists())
                registrationFile.createNewFile();
            writer = new FileWriter(registrationFile);
            writer.append(userName);
            writer.append("\n");
            writer.append(password);
            writer.flush();
        } catch (Exception e) {

        } finally {
            try {
                writer.close();
            } catch (IOException e) {

            }
        }
    }

    public static User readDBFile(File dbFile) {
        User user = new User();
        BufferedReader reader = null;
        try {
            if (null != dbFile) {
                reader = new BufferedReader(new FileReader(dbFile));
                String sCurrentLine;
                while ((sCurrentLine = reader.readLine()) != null) {
                    if (!user.isUserNameSet())
                        user.setUserName(sCurrentLine);
                    else {
                        user.setPassword(sCurrentLine);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return user;
    }
}
