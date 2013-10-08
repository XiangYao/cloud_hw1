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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DetachVolumeRequest;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;


public class EBS_S3 {

    /*
     * Important: Be sure to fill in your AWS access credentials in the
     *            AwsCredentials.properties file before you try to run this
     *            sample.
     * http://aws.amazon.com/security-credentials
     */

    static AmazonEC2      ec2;
    static AmazonS3Client s3;

    public static void main(String[] args) throws Exception {


    	 AWSCredentials credentials = new PropertiesCredentials(
    			 EBS_S3.class.getResourceAsStream("AwsCredentials.properties"));

         /*********************************************
          *  #1 Create Amazon Client object
          **********************************************/
    	 ec2 = new AmazonEC2Client(credentials);

         
         // We assume that we've already created an instance. Use the id of the instance.
         String instanceId = ""; //put your own instance id to test this code.
         
         try{
       
        	/*********************************************
            *  #2.1 Create a volume
            *********************************************/
         	//create a volume
        	CreateVolumeRequest cvr = new CreateVolumeRequest();
	        cvr.setAvailabilityZone("us-east-1a");
	        cvr.setSize(10); //size = 10 gigabytes
        	CreateVolumeResult volumeResult = ec2.createVolume(cvr);
        	String createdVolumeId = volumeResult.getVolume().getVolumeId();
         	
        	
        	/*********************************************
            *  #2.2 Attach the volume to the instance
            *********************************************/
        	AttachVolumeRequest avr = new AttachVolumeRequest();
        	avr.setVolumeId(createdVolumeId);
        	avr.setInstanceId(instanceId);
        	avr.setDevice("/dev/sdf");
        	ec2.attachVolume(avr);
        	
        	/*********************************************
            *  #2.3 Detach the volume from the instance
            *********************************************/
        	DetachVolumeRequest dvr = new DetachVolumeRequest();
        	dvr.setVolumeId(createdVolumeId);
        	dvr.setInstanceId(instanceId);
        	ec2.detachVolume(dvr);
        	
        	
            /************************************************
            *    #3 S3 bucket and object
            ***************************************************/
            s3  = new AmazonS3Client(credentials);
            
            //create bucket
            String bucketName = "cloud-sample-bucket";
            s3.createBucket(bucketName);
            
            //set key
            String key = "object-name.txt";
            
            //set value
            File file = File.createTempFile("temp", ".txt");
            file.deleteOnExit();
            Writer writer = new OutputStreamWriter(new FileOutputStream(file));
            writer.write("This is a sample sentence.\r\nYes!");
            writer.close();
            
            //put object - bucket, key, value(file)
            s3.putObject(new PutObjectRequest(bucketName, key, file));
            
            //get object
            S3Object object = s3.getObject(new GetObjectRequest(bucketName, key));
            BufferedReader reader = new BufferedReader(
            	    new InputStreamReader(object.getObjectContent()));
            String data = null;
            while ((data = reader.readLine()) != null) {
                System.out.println(data);
            }
            
            
            
            /*********************************************
             *  #4 shutdown client object
             *********************************************/
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