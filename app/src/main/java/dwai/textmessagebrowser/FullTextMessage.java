package dwai.textmessagebrowser;

import android.telephony.SmsManager;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import dwai.textmessagebrowser.exceptions.TextMessageNotRecievedException;


public class FullTextMessage {

    private ArrayList<String> texts = new ArrayList<String>();
    private final int EVERYTHING_WORKED = -1;
    public String from;
    public String to;

    public FullTextMessage() {
    }

    public FullTextMessage(String message) {
        this(message, 0);
    }

    public FullTextMessage(String message, int hash) {
        hash %= 1000;
        try {
            message = gzip(message);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        int size = message.length();
        int totalLength = (int) Math.ceil(size / 130.0);
        int i;
        for (i = 0; i < size - 130; i += 130) {
            texts.add((i / 130) + "%" + message.substring(i, i + 130) + "*" + totalLength + "|" + hash);
        }
        texts.add((i / 130) + "%" + message.substring(i) + "*" + totalLength + '|' + hash);
    }

    public int getSize() {
        return texts.size();
    }

    public String toString() {
        return texts.toString();
    }

    public void setHashCode(int hash) {
        hash %= 1000;
        for (String text: texts) {
            int locationHash = text.indexOf('|');
            if (locationHash >= 0) {
                text = text.substring(0, locationHash);
            }
            text += "|";
            text += hash;
        }
    }

    private String gzip(String message) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(out);
        gzipOutputStream.write(message.getBytes());
        gzipOutputStream.close();
        byte[] bytes = out.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    public void clear() {
        texts.clear();
        from = null;
        to = null;
    }

    private int getWrongIndex(){
        for(int i = 0; i < texts.size()-1;i++){
            int currentMessage = Integer.parseInt(texts.get(i));
            int secondMessage = Integer.parseInt(texts.get(i+1));
            if(secondMessage - currentMessage != 0){
                return currentMessage;
            }

        }
        return EVERYTHING_WORKED;
    }

    private boolean basicIsComplete(int EOFIndex){
        return (EOFIndex - texts.size() == 0);


    }

    public void addText(String value) throws Exception {
            texts.add(value);

    }
    private int getMessageNum(String text){
//       Log.d("COSMOS", text);
      if(text.indexOf("*") > 0){
        return -1;
      } else if(text.indexOf("%") > 0) {
//           Log.d("COSMOS", "TEXT:\t" + text.substring(0, text.indexOf("%")));
           int messageNum = Integer.parseInt(text.substring(0, text.indexOf("%")));
//           Log.d("COSMOS", "\t.\n------------\nINT:\t" + messageNum + "\n------------");
           return messageNum;
       } else {
          return -1;
      }

    }


    public String getAllMessages() throws TextMessageNotRecievedException {

//        Log.d("COSMOS", "getAllMessages() Called");

        String combinedHTML = "";
//        Log.d("COSMOS", "Texts:\t"+texts);
        for(String s : texts) {
            String other = s;
            if (s.indexOf('%') >= 0) {
                other = s.substring(s.indexOf('%'));
            }
            if (other.indexOf('|') >= 0) {
                other = other.substring(0, other.indexOf('|'));
            }
            if (other.indexOf('*') >= 0) {
                other = other.substring(0, other.indexOf('*'));
            }
            combinedHTML+=other;
        }

        //Log.d("COSMOS", "Combined HTML:\t"+combinedHTML);

        return combinedHTML;
    }

    private String decompress(byte[] compressed) throws IOException {
        final int BUFFER_SIZE = compressed.length*2;
        ByteArrayInputStream is = new ByteArrayInputStream(compressed);
        GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE);
        StringBuilder string = new StringBuilder();
        byte[] data = new byte[BUFFER_SIZE];
        int bytesRead;
        try {
            while ((bytesRead = gis.read(data)) != -1) {
                string.append(new String(data, 0, bytesRead));
            }
        }
        catch(Exception q){
             q.printStackTrace();

        }
        finally{
            gis.close();
            is.close();
        }
//        Log.d("COSMOS", string.toString());
        return string.toString();
    }

    public String getDecompressedMessages(){
        String allData = getAllMessages();
        if(allData.indexOf("*") > 0) {
            allData.substring(0, allData.indexOf("*"));
        }
//        byte[] compressedData = (Base64.decode("H4sIAAAAAAAAA5WTX0/CMBTFv0rTZ1lDJISQ0kQUQR+MITwQX0y3FVrpWmgvG3x77/gXMWLgZelud865/e2Wayis4KnPt4JHlYHxTnDdFFySzMoYexSrQQElOqhZj1rjFjGpVVQ8uIUBMpbuSzrOpOCsFjpZHqXa5LlyjViQw2oTKbrf1+4gw1xBj36mVroFPUqWa2sbVs1OgRpg2WUMNAYnsk5MjKeib/28ziR/WJ0Jq6pK5gb0Ok0yX7CdQ6BiaGC0Ti85/GwmmLn+3U1tCpUBUOGH6ycVk33xQAMPyhDHGZPSRJNaVUM5Lv+hchWDfdIVBpdZ6D2Lm5xuAcBOszXzHvcxpCX4SrzMyBKx4A7JgylVJFu/viNWAQlKRixrb3MCWuG7cTHhbIV/LDOgRF/h4BXGkeeA8TiYnO3qmIveKWbk5oQ9hQwhx0JaK/qTxy5p9sf5JAw7g+3k7f11My3V6qk96HwU02q0beXjdolN7z7nDH3weeyc7W8L212db7CoiktBAwAA", Base64.DEFAULT));
//        Log.d("COSMOS", "all the data " + "***********1*" + allData + "*1*********");
        byte[] compressedData = Base64.decode(allData, Base64.DEFAULT);
        String data = "";
        try{
            data = decompress(compressedData);
        }
        catch(IOException e ){
           e.printStackTrace();
        }
        String realHTML = data;
//        Log.d("COSMOS", ".\nREAL HTML:\n"+data);

        return realHTML;
    }
    public static void closeQuietly(InputStream is) {
        try {
            if (is != null) {
                is.close();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }

    private String getContentFromMessage(String message){
        int indexOfFirstPercent = message.indexOf("%");
        int indexOfLastPercent = message.lastIndexOf("%");

        if(indexOfFirstPercent == indexOfLastPercent){
            //There is no EOF
            return message.substring(indexOfFirstPercent+1);
        }
        //substring is exclusive
        return message.substring(indexOfFirstPercent+1,indexOfLastPercent);

    }

    public String messageAt(int index) {
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            if (index == Integer.parseInt(text.substring(0, text.indexOf('%')))) {
                return text;
            }
        }
        return null;
    }

    public void sortMessages() {
        //Log.d("COSMOS", "Texts Size:\t"+texts.size());
        String[] temp = new String [texts.size()];
        for(String s: texts)
        {
            int index = Integer.parseInt(s.substring(0,s.indexOf("%")));
            if(s.contains("*"))
                temp[index] = s.substring(s.indexOf("%") + 1, s.indexOf("*"));
            else
                temp[index] = s.substring(s.indexOf("%") + 1);
        }

        for(int i=0;i<temp.length;i++)
        {
            texts.set(i,temp[i]);
        }
    }

    public void send() {
        SmsManager sms = SmsManager.getDefault();
        Log.d("COSMOS", "Sending " + getAllMessages() + ", decoded to " + getDecompressedMessages());
        for (String text: texts) {
            sms.sendTextMessage(to, null, text, null, null);
        }
    }
}