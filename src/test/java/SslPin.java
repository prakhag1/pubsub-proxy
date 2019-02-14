import com.squareup.okhttp.CertificatePinner;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

public class SslPin {

	public static void main (String [] args) {
		
		try {
			String hostname = "google.com";
			CertificatePinner certificatePinner = new CertificatePinner.Builder()
			  .add(hostname, "sha1/AAAAAAAAAAAAAAAAAAAAAAAAAAA=")
			  .build();
			  
			OkHttpClient client = new OkHttpClient();
			client.setCertificatePinner(certificatePinner);
	
			Request request = new Request.Builder()
			  .url("https://" + hostname)
			  .build();
			
			client.newCall(request).execute();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
