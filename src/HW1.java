import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import java.io.BufferedReader;
/*
 * Copyright 2010 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * Modified by Sambit Sahu
 * Modified by Kyung-Hwa Kim (kk2515@columbia.edu)
 * 
 * 
 */
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.security.Security;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.PKCS8KeyFile;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateImageRequest;
import com.amazonaws.services.ec2.model.CreateImageResult;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.CreateSnapshotRequest;
import com.amazonaws.services.ec2.model.CreateSnapshotResult;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DeleteVolumeRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.DetachVolumeRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;


public class HW1 {

    /*
     * Important: Be sure to fill in your AWS access credentials in the
     *            AwsCredentials.properties file before you try to run this
     *            sample.
     * http://aws.amazon.com/security-credentials
     */

    static AmazonEC2      ec2;
    static AmazonS3Client s3;
    
    public static String createAmiFromInstance(String instanceId, String name, boolean noReboot) {
        CreateImageRequest createImageRequest = new CreateImageRequest();
        createImageRequest.setInstanceId(instanceId);
        createImageRequest.setName(name);
        createImageRequest.setNoReboot(noReboot);
        CreateImageResult createImageResult = ec2.createImage(createImageRequest);
        String imageId = createImageResult.getImageId();
        ArrayList<String> imageIds = new ArrayList<String>();
        imageIds.add(imageId);
        
        try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        
        return imageId;
    }
    
    public static String createSnapShotFromVolume(String volumeId) {
        CreateSnapshotRequest createsanpShotRequest1 = new CreateSnapshotRequest();
        createsanpShotRequest1.setVolumeId(volumeId);
        CreateSnapshotResult createSnapshotresult1 = ec2.createSnapshot(createsanpShotRequest1);
        String snapshotid = createSnapshotresult1.getSnapshot().getSnapshotId();
        
        return snapshotid;
    }
    
    public static String getInstanceZone(String instanceId) {
    	List<String> instanceIds = new ArrayList<String>();
    	instanceIds.add(instanceId);
        DescribeInstancesRequest describeInstanceRequest = new DescribeInstancesRequest();
        describeInstanceRequest.setInstanceIds(instanceIds);
        DescribeInstancesResult describeInstancesResult = ec2.describeInstances(describeInstanceRequest);
        Instance instance = describeInstancesResult.getReservations().get(0).getInstances().get(0);
        return instance.getPlacement().getAvailabilityZone();
    }
    
    public static String getInstanceIP(String instanceId) {
    	List<String> instanceIds = new ArrayList<String>();
    	instanceIds.add(instanceId);
        DescribeInstancesRequest describeInstanceRequest = new DescribeInstancesRequest();
        describeInstanceRequest.setInstanceIds(instanceIds);
        DescribeInstancesResult describeInstancesResult = ec2.describeInstances(describeInstanceRequest);
        Instance instance = describeInstancesResult.getReservations().get(0).getInstances().get(0);
        return instance.getPublicIpAddress();
    }
    
    public static String getVolumeState(String volumeId) {
    	DescribeVolumesRequest dv = new DescribeVolumesRequest();
    	List<String> v_list = new ArrayList<String>();
    	v_list.add(volumeId);
    	dv.setVolumeIds(v_list);
    	DescribeVolumesResult dr = ec2.describeVolumes(dv);
    	String state = dr.getVolumes().get(0).getState();
    	return state;
    }
    
    public static void deatchVolume(String instanceId, String volumeId) {
    	DetachVolumeRequest dvr = new DetachVolumeRequest();
    	dvr.setVolumeId(volumeId);
    	dvr.setInstanceId(instanceId);
    	ec2.detachVolume(dvr);
    }
    
    public static void deleteVolume(String volumeId) {
    	DeleteVolumeRequest deleteVolumeRequest1 = new DeleteVolumeRequest();
    	deleteVolumeRequest1.setVolumeId(volumeId);
    	ec2.deleteVolume(deleteVolumeRequest1);
    }
    
    public static String createVolume(String zone, String snapshotId) {
    	String volume_name;
    	CreateVolumeRequest cvr = new CreateVolumeRequest();
	    cvr.setSize(10); //size = 10 gigabytes
        CreateVolumeResult volume_result;
      	cvr.setAvailabilityZone(zone);
      	if (snapshotId != null)
      		cvr.setSnapshotId(snapshotId);
      	volume_result = ec2.createVolume(cvr);
      	volume_name = volume_result.getVolume().getVolumeId();
    	
    	return volume_name;
    }
    
    public static void attachVolume(String instanceId, String volumeId) {
    	AttachVolumeRequest avr = new AttachVolumeRequest();
     	/* attach instances to existing volume */
     	avr.setDevice("/dev/sdf");
     	avr.setVolumeId(volumeId);
 		avr.setInstanceId(instanceId);
 		ec2.attachVolume(avr);
 		System.out.println("New instance: " + instanceId + " has been attached to volumn: " + volumeId);
    }
    
    
    public static void execCmdRuby(String instanceDNS, String keyGroupName) {
    	System.out.println("Client is ready, please scp the script file the instance with DNS: " + instanceDNS);
        System.out.println("Type 'ok' to continue...");
        Scanner scan = new Scanner(System.in);
        String ok = scan.nextLine();
        System.out.println("Start to run the script");
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        SSHClient client = new SSHClient();
        client.addHostKeyVerifier(new PromiscuousVerifier());
        try {
			client.connect(instanceDNS);
			PKCS8KeyFile keyFile = new PKCS8KeyFile();
	        keyFile.init(new File("/Users/will/.ssh/" + keyGroupName + ".pem"));
	        client.authPublickey("ec2-user",keyFile);
	        System.out.println("Successfully logged in, running command 'ruby fib.rb' now...");
	        final Session session = client.startSession();
	        final Command cmd = session.exec("ruby fib.rb > firstday.txt");
	        String response = IOUtils.readFully(cmd.getInputStream()).toString();
	        cmd.join(10, TimeUnit.SECONDS);
	        System.out.println(response);
	        session.close();
	        client.disconnect();   
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public static void execCmdRead(String instanceDNS, String keyGroupName) {
        System.out.println("Start to read the data stored yesterday...");
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        SSHClient client = new SSHClient();
        client.addHostKeyVerifier(new PromiscuousVerifier());
        try {
			client.connect(instanceDNS);
			PKCS8KeyFile keyFile = new PKCS8KeyFile();
	        keyFile.init(new File("/Users/will/.ssh/" + keyGroupName + ".pem"));
	        client.authPublickey("ec2-user",keyFile);
	        System.out.println("Successfully logged in, running command 'cat firstday.txt' now...");
	        final Session session = client.startSession();
	        final Command cmd = session.exec("cat firstday.txt");
	        String response = IOUtils.readFully(cmd.getInputStream()).toString();
	        cmd.join(10, TimeUnit.SECONDS);
	        System.out.println(response);
	        session.close();
	        client.disconnect();   
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public static String createInstanceFromImageId(String imageId, String keyGroupName) {
        int minInstanceCount = 1;
        int maxInstanceCount = 1;
        boolean ready = false;
     	/* create instances */
        RunInstancesRequest rir = new RunInstancesRequest(imageId, minInstanceCount, maxInstanceCount);
        rir.withKeyName(keyGroupName).withSecurityGroups(keyGroupName);
        RunInstancesResult runresult = ec2.runInstances(rir);
        
        Reservation reservation = runresult.getReservation();
        List<Instance> allInstances = reservation.getInstances();
        String insId = allInstances.get(0).getInstanceId();
        
        DescribeInstancesResult describeInstancesRequest;
        List<Reservation> reservations;
        allInstances = new ArrayList<Instance>();
        
        while (!ready) {
        	describeInstancesRequest = ec2.describeInstances();
        	reservations = describeInstancesRequest.getReservations();
        	for (Reservation r : reservations) {
        		for (Instance ins : r.getInstances()) {
        			if (ins.getState().getName().compareTo("running") == 0 && ins.getInstanceId().compareTo(insId) == 0) {
        				ready = true;
        			}
        		}
            }
        }
        return insId;
    }
    
    
    public static void main(String[] args) throws Exception {


    	 AWSCredentials credentials = new PropertiesCredentials(
    			 HW1.class.getResourceAsStream("AwsCredentials.properties"));

    	 
         /*********************************************
          * 
          *  #1 Create Amazon Client object
          *  
          *********************************************/
    	 System.out.println("#1 Create Amazon Client object");
         ec2 = new AmazonEC2Client(credentials);
    	 ec2.setEndpoint("https://us-east-1.ec2.amazonaws.com");

         System.out.println("Please enter required group name and key name... (consider them to be the same)");
         Scanner scan = new Scanner(System.in);
         final String keyGroupName = scan.nextLine();
         
         /* create security group */
         CreateSecurityGroupRequest createSecurityGroupRequest = new CreateSecurityGroupRequest();
         createSecurityGroupRequest.withGroupName(keyGroupName).withDescription("My Java Security Group");
         CreateSecurityGroupResult createSecurityGroupResult = ec2.createSecurityGroup(createSecurityGroupRequest);
         
         /* set ip settings */
         IpPermission ipPermission = new IpPermission();
         /* authorize tcp, ssh 22 */
         ipPermission.withIpRanges("0.0.0.0/0")
         			 .withIpProtocol("tcp")
        		     .withFromPort(22)
         /* authorize http 80 */
        		     .withToPort(80);
         
         AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest = new AuthorizeSecurityGroupIngressRequest();
         authorizeSecurityGroupIngressRequest.withGroupName(keyGroupName)
        		                             .withIpPermissions(ipPermission);
         ec2.authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);
                  
         /* create key pair */
         CreateKeyPairRequest createKeyPairRequest = new CreateKeyPairRequest();
         createKeyPairRequest.withKeyName(keyGroupName);
         CreateKeyPairResult createKeyPairResult = ec2.createKeyPair(createKeyPairRequest);
         KeyPair keyPair = new KeyPair();
         keyPair = createKeyPairResult.getKeyPair();
         String privateKey = keyPair.getKeyMaterial();
         PrintWriter file = new PrintWriter("/Users/will/.ssh/" + keyGroupName + ".pem");
         file.print(privateKey);
         file.close();
         Runtime.getRuntime().exec("chmod 400 /Users/will/.ssh/" + keyGroupName + ".pem");      

        try {
            
            /*********************************************
             * 
             *  #2 Create two Instances
             *  
             *********************************************/
            System.out.println();
            System.out.println("#2 Create two new Instances");
            int ready_num = 0;
            String insDNS1 = new String();
            String insDNS2 = new String();
            String insId1 = new String();
            String insId2 = new String();
            String insZone1 = new String();
            String insZone2 = new String();
            
            String imageId = "ami-76f0061f"; //Basic 32-bit Amazon Linux AMI
            int minInstanceCount = 2; // create 2 instance
            int maxInstanceCount = 2;
            
         	/* create instances */
            RunInstancesRequest rir = new RunInstancesRequest(imageId, minInstanceCount, maxInstanceCount);
            rir.withKeyName(keyGroupName).withSecurityGroups(keyGroupName);
            ec2.runInstances(rir);
            
            /* waiting for instance to start */
            System.out.println("Created instance, wait for pending...");
         	 
            DescribeInstancesResult describeInstancesRequest;
            List<Reservation> reservations;
            List<Instance> allInstances = new ArrayList<Instance>();
            
            while (ready_num < 2) {
            	describeInstancesRequest = ec2.describeInstances();
            	reservations = describeInstancesRequest.getReservations();
            	for (Reservation reservation : reservations) {
            		for (Instance ins : reservation.getInstances()) {
            			if (ins.getState().getName().compareTo("running") == 0 && ins.getPublicIpAddress() != null) {
            				if (allInstances.size() == 0 || (allInstances.size() > 0 && allInstances.get(0).getInstanceId().compareTo(ins.getInstanceId())!=0)) {
            					ready_num++;
            					allInstances.add(ins);
            				}
            			}
            		}
                }
            }
            
            System.out.println("You have " + allInstances.size() + " Amazon EC2 instance(s).");
            insId1 = allInstances.get(0).getInstanceId();
            insId2 = allInstances.get(1).getInstanceId();
            insDNS1 = allInstances.get(0).getPublicIpAddress();
            insDNS2 = allInstances.get(1).getPublicIpAddress();
            insZone1 = allInstances.get(0).getPlacement().getAvailabilityZone();
            insZone2 = allInstances.get(1).getPlacement().getAvailabilityZone();
            
            for (Instance ins : allInstances){
            	System.out.println("New instance has been created: "+ins.getInstanceId());
            }
            
            System.out.println("Both instances are running now:");
            System.out.println("Instance id1: " + insId1);
            System.out.println("IP: " + insDNS1);
            System.out.println("Zone: " + insZone1);
            System.out.println("Instance id1: " + insId2);
            System.out.println("IP: " + insDNS2);
            System.out.println("Zone: " + insZone2);
            System.out.println();
            
            
            /*********************************************
             *  #3 Check OR Create two volumes
             *********************************************/
            System.out.println();
            System.out.println("#3 Create volumes");
            String volume_name1 = createVolume(insZone1, null);
            String volume_name2 = createVolume(insZone2, null);
     	    
            /*********************************************
             *  #4 Attach the volume to the instance
             *********************************************/
            System.out.println();
         	System.out.println("#4 Attach the volume to the instance");
         	System.out.println("Wait for volumes to be available...");
            Thread.sleep(20000);
         	
         	/* attach instances to existing volume */
         	attachVolume(insId1, volume_name1);
         	attachVolume(insId2, volume_name2);
     		
         	 /************************************************
             *  #5 S3 bucket and object
             ***************************************************/
         	System.out.println();
         	System.out.println("#5 S3 bucket and object");
         	s3  = new AmazonS3Client(credentials);
             
            /* create bucket */
            String bucketName = "cloud-hw1-bucket";
            s3.createBucket(bucketName);
             
            /* set key */
            String key = "object-hw1.txt";
             
            /* set value */
            File new_file = File.createTempFile("temp", ".txt");
            new_file.deleteOnExit();
            Writer writer = new OutputStreamWriter(new FileOutputStream(new_file));
            writer.write("This is the file stored on the S3 storage on the first day!!!.");
            writer.close();
            
            /* put object - bucket, key, value(file) */
            s3.putObject(new PutObjectRequest(bucketName, key, new_file));
            System.out.println("Successfully put file temp.txt to S3, we will read it tomorrow...");
            System.out.println();
                        
            /***********************************
			 *   #3 Monitoring (CloudWatch)
			 *********************************/
            System.out.println();
            System.out.println("#6 set up cloudwatch");
			try {
				/* create CloudWatch client */
				AmazonCloudWatchClient cloudWatch = new AmazonCloudWatchClient(credentials) ;
				/* create request message1 */
				GetMetricStatisticsRequest statRequest1 = new GetMetricStatisticsRequest();
				GetMetricStatisticsRequest statRequest2 = new GetMetricStatisticsRequest();
				/* set up request message */
				statRequest1.setNamespace("AWS/EC2"); //namespace
				statRequest2.setNamespace("AWS/EC2"); //namespace
				statRequest1.setPeriod(60); //period of data
				statRequest2.setPeriod(60); //period of data
				ArrayList<String> stats = new ArrayList<String>();
				/* Use one of these strings: Average, Maximum, Minimum, SampleCount, Sum */
				stats.add("Average"); 
				stats.add("Sum");
				statRequest1.setStatistics(stats);
				statRequest2.setStatistics(stats);
				/* Use one of these strings: CPUUtilization, NetworkIn, NetworkOut, DiskReadBytes, DiskWriteBytes, DiskReadOperations  */
				statRequest1.setMetricName("CPUUtilization");
				statRequest2.setMetricName("CPUUtilization");
				/* set time */
				GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
				calendar.add(GregorianCalendar.SECOND, -1 * calendar.get(GregorianCalendar.SECOND)); // 1 second ago
				Date endTime = calendar.getTime();
				calendar.add(GregorianCalendar.MINUTE, -10); // 10 minutes ago
				Date startTime = calendar.getTime();
				statRequest1.setStartTime(startTime);
				statRequest1.setEndTime(endTime);
				statRequest2.setStartTime(startTime);
				statRequest2.setEndTime(endTime);
				/* specify an instance */
				ArrayList<Dimension> dimensions1 = new ArrayList<Dimension>();
				dimensions1.add(new Dimension().withName("InstanceId").withValue(insId1));
				ArrayList<Dimension> dimensions2 = new ArrayList<Dimension>();
				dimensions2.add(new Dimension().withName("InstanceId").withValue(insId2));
				statRequest1.setDimensions(dimensions1);
				statRequest2.setDimensions(dimensions2);
				System.out.println("Set up cloud watch for instance: " + insId1 + " and instance: " + insId2);
				
				/* !!!!!!!!!!!!here set for 10 loops for now */
				/* get statistics */
				for (int i=0; i<10; i++) {
					GetMetricStatisticsResult statResult1 = cloudWatch.getMetricStatistics(statRequest1);
					GetMetricStatisticsResult statResult2 = cloudWatch.getMetricStatistics(statRequest2);
					/* display */
					System.out.println("Instance 1: " + statResult1.toString());
					List<Datapoint> dataList = statResult1.getDatapoints();
					Double averageCPU = null;
					Date timeStamp = null;
					for (Datapoint d : dataList) {
						averageCPU = d.getAverage();
						timeStamp = d.getTimestamp();
						System.out.println("Instance 1 average CPU utlilization for last 10 minutes: "+averageCPU);
						System.out.println("Instance 1 total CPU utlilization for last 10 minutes: "+d.getSum());
					}
					System.out.println();
					System.out.println("Instance 2: " + statResult1.toString());
					dataList = statResult2.getDatapoints();
					for (Datapoint d : dataList) {
						averageCPU = d.getAverage();
						timeStamp = d.getTimestamp();
						System.out.println("Instance 2 average CPU utlilization for last 10 minutes: "+averageCPU);
						System.out.println("Instance 2 total CPU utlilization for last 10 minutes: "+d.getSum());
					}
				}
				
			} catch (AmazonServiceException ase) {
			    System.out.println("Caught Exception: " + ase.getMessage());
			    System.out.println("Reponse Status Code: " + ase.getStatusCode());
			    System.out.println("Error Code: " + ase.getErrorCode());
			    System.out.println("Request ID: " + ase.getRequestId());
			}
			
            
            /***********************************
			 *   # Copy script to 
			 *    	instance and run
			 *********************************/
			System.out.println();
            System.out.println("Waiting for init and automatically SSH...");                        
            /* call runtime exec to run scp */
            execCmdRuby(insDNS1, keyGroupName);

            
            /***********************************
			 *   # Save instances to image
			 *********************************/
            System.out.println();
            System.out.println("******* Approaching 5:00 pm, create ami for instances *********");
            String imageId1;
            String imageId2;
            String snapshot1;
            String snapshot2;
            
            imageId1 = createAmiFromInstance(insId1, "image1", true);
            imageId2 = createAmiFromInstance(insId2, "image2", true);
            System.out.println("Created first image id: " + imageId1);
            System.out.println("Created second image id: " + imageId2);
            
            snapshot1 = createSnapShotFromVolume(volume_name1);
            snapshot2 = createSnapShotFromVolume(volume_name2);
            System.out.println("Created first snapshot id: " + snapshot1);
            System.out.println("Created second snapshot id: " + snapshot2);
            
            
            /*********************************************
             * 
             *  # Stop Instances
             *  
             *********************************************/
            System.out.println();
            System.out.println("#7 Stop & terminate the Instance");
            List<String> instanceIds = new LinkedList<String>();
            instanceIds.add(insId1);
            instanceIds.add(insId2);
            /* stop instances */
            StopInstancesRequest stopIR = new StopInstancesRequest(instanceIds);
            ec2.stopInstances(stopIR);
            TerminateInstancesRequest tir = new TerminateInstancesRequest(instanceIds);
            ec2.terminateInstances(tir);
            
            /*********************************************
             * 
             *  # Detach volumes
             *  
             *********************************************/
            System.out.println();
            System.out.println("Detach the volumes from the instances...");
            deatchVolume(insId1, volume_name1);
        	deatchVolume(insId2, volume_name2);
            
        	/*********************************************
             * 
             *  # Delete Volumes
             *  
             *********************************************/
        	System.out.println();
        	
        	while (true) {
	        	if (getVolumeState(volume_name1).compareTo("available") == 0 && getVolumeState(volume_name2).compareTo("available") == 0)
	        		break;
	        }
        	System.out.println("Delete volumes...");
        	Thread.sleep(10000);
        	deleteVolume(volume_name1);
        	deleteVolume(volume_name2);
            
        	/*********************************************
             * 
             *  # Second day restore instances and volumes
             *  
             *********************************************/
        	System.out.println();
        	System.out.println("#8 Second day start up instances from stored amis...");
        	String newInsId1 = "";
        	String newInsId2 = "";
        	String newInsIP1 = "";
        	String newInsIP2 = "";
        	String newInsZone1 = "";
        	String newInsZone2 = "";
        	
        	newInsId1 = createInstanceFromImageId(imageId1, keyGroupName);
        	newInsId2 = createInstanceFromImageId(imageId2, keyGroupName);
        	System.out.println("Second day first instance has been restored with id: " + newInsId1);
        	System.out.println("Second day second instance has been restored with id: " + newInsId2);
        	newInsZone1 = getInstanceZone(newInsId1);
        	newInsZone2 = getInstanceZone(newInsId2);
        	System.out.println("New instance 1 zone: " + newInsZone1);
        	System.out.println("New instance 2 zone: " + newInsZone2);
        	newInsIP1 = getInstanceIP(newInsId1);
        	newInsIP2 = getInstanceIP(newInsId2);
        	System.out.println("New instance 1 IP: " + newInsIP1);
        	System.out.println("New instance 2 IP: " + newInsIP2);      	

            Thread.sleep(120000);
            /* exec read */
            System.out.println();
            System.out.println("Now start to read the file stored yesterday...");
        	execCmdRead(newInsIP1, keyGroupName);

            /*********************************************
             *  
             *  #9 Read data from S3
             *  
             *********************************************/     
        	
            /* get the object from the first day */
        	System.out.println();
        	System.out.println("#9 Reading data from S3 stored on the first day");
            S3Object object = s3.getObject(new GetObjectRequest(bucketName, key));
            BufferedReader reader = new BufferedReader(
            	    new InputStreamReader(object.getObjectContent()));
            String data = null;
            while ((data = reader.readLine()) != null) {
                System.out.println(data);
            }
        	
        	
            /*********************************************
             *  
             *  #10 shutdown client object
             *  
             *********************************************/            
            System.out.println("#10 shutdown client objects");
            ec2.shutdown();
            s3.shutdown();
            
        } catch (AmazonServiceException ase) {
                System.out.println("Caught Exception: " + ase.getMessage());
                System.out.println("Reponse Status Code: " + ase.getStatusCode());
                System.out.println("Error Code: " + ase.getErrorCode());
                System.out.println("Request ID: " + ase.getRequestId());
        }

        
    }
}
