import json
import boto3
import os
import time
import urllib.parse


destbucketName = ''
createdS3Document = ''


client = boto3.client('s3')
textract = boto3.client('textract')

#Get Data from s3
def getTextractData(bucketName, key):
    response = None
    client = boto3.client('textract')
    response = client.start_document_text_detection(
        DocumentLocation={
            'S3Object': {
                'Bucket': bucketName,
                'Name': key
            }
        })

    return response["JobId"]
    
def isJobComplete(jobId):
    time.sleep(5)
    client = boto3.client('textract')
    response = client.get_document_text_detection(JobId=jobId)
    status = response["JobStatus"]
    print("Job status: {}".format(status))
    while (status == "IN_PROGRESS"):
        time.sleep(5)
        response = client.get_document_text_detection(JobId=jobId)
        status = response["JobStatus"]
        print("Job status: {}".format(status))
    return status
    
def getJobResults(jobId):
    pages = []
    client = boto3.client('textract')
    response = client.get_document_text_detection(JobId=jobId)
    pages.append(response)
    print("Resultset page recieved: {}".format(len(pages)))
    nextToken = None
    if ('NextToken' in response):
        nextToken = response['NextToken']
    while (nextToken):
        response = client.get_document_text_detection(JobId=jobId, NextToken=nextToken)
        pages.append(response)
        print("Resultset page recieved: {}".format(len(pages)))
        nextToken = None
        if ('NextToken' in response):
            nextToken = response['NextToken']
    return pages

def lambda_handler(event, context):
    bucketName = event['Records'][0]['s3']['bucket']['name']
    key = urllib.parse.unquote_plus(event['Records'][0]['s3']['object']['key'], encoding='utf-8')
    try:
        jobId = getTextractData(bucketName, key)
        detectedText = ''
        print("Started job with id: {}".format(jobId))
        if (isJobComplete(jobId)):
            response = getJobResults(jobId)
            #Get the detected text
        for resultPage in response:
            for item in resultPage["Blocks"]:
                if item["BlockType"] == "LINE":
                    detectedText += item["Text"] + '\n'
        generateFilePath = os.path.splitext(createdS3Document)[0] + '.txt'
        client.put_object(Body=detectedText, Bucket=destbucketName, Key=generateFilePath)
        print('Generated ' + generateFilePath)

    except Exception as e:
        print(e)
