import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class Zero {

    private String serviceAccountKey = "xxx";
    private String serviceAccountSecret = "xxx";
    private String projectId = "xxx";

    private String apiUrlBase = "https://api.disruptive-technologies.com/v2beta1";
    private String emulatorUrlBase = "https://emulator.disruptive-technologies.com/v2beta1";
    private String apiDeviceUrl = apiUrlBase + "/projects/" + projectId + "/devices";
    private String emulatedDeviceUrl = emulatorUrlBase + "/projects/" + projectId + "/devices";
    private String codeExampleSensorDisplayName = "Java Code Example Touch Sensor";
    private String createEmulatedSensorJSON =
            "{\n" + "  \"type\": \"touch\",\n" + "  \"labels\": {\n" + "    \"name\": \"" + codeExampleSensorDisplayName + "\",\n" + "    \"virtual-sensor\": \"\"\n" + "  }\n" +
                    "}\n" + "publishEmulatedTouchJSON = {\n" + "  \"touch\": {\n" + "    \"touch\": {\n" + "    }\n" + "  }\n" + "}";
    private String publishEmulatedTouchJSON = "{\n" + "  \"touch\": {\n" + "    \"touch\": {\n" + "    }\n" + "  }\n" + "}";

    public static void main(String[] args) {
        Zero zero = new Zero();
        try {
            String devicePath = zero.getOrCreateTouchSensor();
            zero.generateTouchEvents(devicePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getOrCreateTouchSensor() throws IOException {
        String response = get(apiDeviceUrl + "?label_filters=" + URLEncoder.encode("name=" + codeExampleSensorDisplayName, "UTF-8"));
        String devicePath;
        //Java has no built-in JSON parser, hard coding the check
        String devices = response.substring(response.indexOf("[") + 1, response.indexOf("]"));
        if (!devices.trim().contains(codeExampleSensorDisplayName)) {
            System.out.println("Creating touch sensor with name " + codeExampleSensorDisplayName);
            response = post(emulatedDeviceUrl, createEmulatedSensorJSON);
            devicePath = getDevicePath(response);
        } else {
            System.out.println("Found already existing touch sensor with name " + codeExampleSensorDisplayName);
            devicePath = getDevicePath(devices);
        }

        return devicePath;
    }

    private void generateTouchEvents(String devicePath) throws IOException, InterruptedException {
        System.out.println("Starting to generate one emulated TouchEvent per second... (press CTRL-C to abort)");
        while (true) {
            Thread.sleep(1000);
            System.out.println("Touching Sensor");
            post(emulatorUrlBase + "/" + devicePath + ":publish", publishEmulatedTouchJSON);
        }
    }

    private String get(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        String userpass = serviceAccountKey + ":" + serviceAccountSecret;
        String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes());
        conn.setRequestProperty("Authorization", basicAuth);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Failed : HTTP error code : " + responseCode);
        }
        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        String response = br.readLine();
        conn.disconnect();
        return response;
    }

    private String post(String urlString, String data) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        String userpass = serviceAccountKey + ":" + serviceAccountSecret;
        String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes());
        conn.setRequestProperty("Authorization", basicAuth);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Accept", "application/json");
        OutputStream os = conn.getOutputStream();
        os.write(data.getBytes());
        os.flush();

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Failed : HTTP error code : " + responseCode);
        }
        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        String response = br.readLine();
        conn.disconnect();
        return response;
    }

    private String getDevicePath(String response) {
        //Java has no built-in JSON parser, hard coding the check
        return response.substring(response.indexOf("projects"), response.indexOf("\",\"type\":"));
    }

}
