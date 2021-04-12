import boto3
import time
import os



def startJob(s3BucketName, objectName):
    response = None
    client = boto3.client('textract')
    response = client.start_document_text_detection(
        DocumentLocation={
            'S3Object': {
                'Bucket': s3BucketName,
                'Name': objectName
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

s3BucketName = ''
destbucketName= ''
documentName= '.pdf'
createdS3Document = 'myTextextracted'

jobId = startJob(s3BucketName, documentName)
pdfText = ''
print("Started job with id: {}".format(jobId))
if (isJobComplete(jobId)):
    response = getJobResults(jobId)


# Print detected text
for resultPage in response:
    for item in resultPage["Blocks"]:
        if item["BlockType"] == "LINE":
            #print('\033[94m' + item["Text"] + '\033[0m')
            pdfText += item["Text"] + '\n'

client = boto3.client('s3')


generateFilePath = os.path.splitext(createdS3Document)[0] + '.txt'
client.put_object(Body=pdfText, Bucket=destbucketName, Key=generateFilePath)
print('Generated ' + generateFilePath)
