import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AllocateAddressResult;
import com.amazonaws.services.ec2.model.AssociateAddressRequest;
import com.amazonaws.services.ec2.model.DisassociateAddressRequest;


public class Elastic_IP_CloudWatch {

	public static void main(String[] args) throws Exception{


		AWSCredentials credentials = new PropertiesCredentials(
				Elastic_IP_CloudWatch.class.getResourceAsStream("AwsCredentials.properties"));
		
		/*********************************************
 		*  	#1 Create Amazon Client object
 		*********************************************/
 		System.out.println("#1 Create Amazon Client object");
 		AmazonEC2 ec2 = new AmazonEC2Client(credentials);
 		
 		
		// we assume that we've already created an instance. Use the id of the instance.
		String instanceId = ""; //put your own instance id to test this code.
		
		try{
 			
			/*********************************************
			*  	#2 Allocate elastic IP addresses.
			*********************************************/
			
			//allocate
			AllocateAddressResult elasticResult = ec2.allocateAddress();
			String elasticIp = elasticResult.getPublicIp();
			System.out.println("New elastic IP: "+elasticIp);
				
			//associate
			AssociateAddressRequest aar = new AssociateAddressRequest();
			aar.setInstanceId(instanceId);
			aar.setPublicIp(elasticIp);
			ec2.associateAddress(aar);
			
			//disassociate
			DisassociateAddressRequest dar = new DisassociateAddressRequest();
			dar.setPublicIp(elasticIp);
			ec2.disassociateAddress(dar);
            
        	
			/***********************************
			 *   #3 Monitoring (CloudWatch)
			 *********************************/
			
			//create CloudWatch client
			AmazonCloudWatchClient cloudWatch = new AmazonCloudWatchClient(credentials) ;
			
			//create request message
			GetMetricStatisticsRequest statRequest = new GetMetricStatisticsRequest();
			
			//set up request message
			statRequest.setNamespace("AWS/EC2"); //namespace
			statRequest.setPeriod(60); //period of data
			ArrayList<String> stats = new ArrayList<String>();
			
			//Use one of these strings: Average, Maximum, Minimum, SampleCount, Sum 
			stats.add("Average"); 
			stats.add("Sum");
			statRequest.setStatistics(stats);
			
			//Use one of these strings: CPUUtilization, NetworkIn, NetworkOut, DiskReadBytes, DiskWriteBytes, DiskReadOperations  
			statRequest.setMetricName("CPUUtilization"); 
			
			// set time
			GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
			calendar.add(GregorianCalendar.SECOND, -1 * calendar.get(GregorianCalendar.SECOND)); // 1 second ago
			Date endTime = calendar.getTime();
			calendar.add(GregorianCalendar.MINUTE, -10); // 10 minutes ago
			Date startTime = calendar.getTime();
			statRequest.setStartTime(startTime);
			statRequest.setEndTime(endTime);
			
			//specify an instance
			ArrayList<Dimension> dimensions = new ArrayList<Dimension>();
			dimensions.add(new Dimension().withName("InstanceId").withValue(instanceId));
			statRequest.setDimensions(dimensions);
			
			//get statistics
			GetMetricStatisticsResult statResult = cloudWatch.getMetricStatistics(statRequest);
			
			//display
			System.out.println(statResult.toString());
			List<Datapoint> dataList = statResult.getDatapoints();
			Double averageCPU = null;
			Date timeStamp = null;
			for (Datapoint data : dataList){
				averageCPU = data.getAverage();
				timeStamp = data.getTimestamp();
				System.out.println("Average CPU utlilization for last 10 minutes: "+averageCPU);
				System.out.println("Totl CPU utlilization for last 10 minutes: "+data.getSum());
			}
            
            
            
		} catch (AmazonServiceException ase) {
		    System.out.println("Caught Exception: " + ase.getMessage());
		    System.out.println("Reponse Status Code: " + ase.getStatusCode());
		    System.out.println("Error Code: " + ase.getErrorCode());
		    System.out.println("Request ID: " + ase.getRequestId());
		}
	        
	}
	
}