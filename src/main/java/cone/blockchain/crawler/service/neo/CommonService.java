package cone.blockchain.crawler.service.neo;

import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

@Service
public class CommonService {

    public void getInputData(String inputData, HashMap<String, String> inputDataMap) {
        inputDataMap.put("method", "");
        inputDataMap.put("address", "0x");
        inputDataMap.put("amount", "0");
        /**
         * input data string
         * 0xa9059cbb000000000000000000000000f49b5db19e6a5bd1f97b96c7b9bd7247a42b4fdd00000000000000000000000000000000000000000000000000218f1d54bdcc80
         */
        if (Strings.isNotBlank(inputData) && inputData.length() >= 138) {
            inputDataMap.put("method", inputData.substring(0, 10));
            inputDataMap.put("address", "0x" + inputData.substring(34, 74));
            inputDataMap.put("amount", "0x" + inputData.substring(74, 138));
        }
    }

    public String getHexaToDecimal(String hexa) {

        BigInteger convertValue = BigInteger.ZERO;
        if (hexa.startsWith("0x")) {
            convertValue = new BigInteger(hexa.substring(2), 16);
        } else {
            convertValue = BigInteger.valueOf(Long.valueOf(hexa));
        }

        return String.valueOf(convertValue);
    }

    /**
     * https://stackoverflow.com/questions/17432735/convert-unix-time-stamp-to-date-in-java/27319755
     *
     * @param unixSeconds
     * @return
     */
    public String getEpochTimeToDate(Long unixSeconds) {
        Date date = new Date(unixSeconds * 1000L);
        // the format of your date
//        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // give a timezone reference for formatting (see comment at the bottom)
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        String formattedDate = sdf.format(date);

        return formattedDate;
    }
}
