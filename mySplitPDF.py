import boto3
import botocore
import os
from PyPDF3 import PdfFileWriter, PdfFileReader, pdf

SRC_BUCKET_NAME = 'pdfsplitstore'
pdf_name = 'splitted_pdf'

client = boto3.client('s3')
destbucketName = 'lambdatextractbucket'
documentKey = 'extracted-pdf'


def download_to_s3(myfile):
    s3 = boto3.client('s3')
    s3.download_file(SRC_BUCKET_NAME, myfile, '/tmp/'+myfile)


def split_pdf(myfile):
    pdf_in_file = open('/tmp/'+myfile, 'rb')
    inputpdf = PdfFileReader(pdf_in_file)
    pages_no = inputpdf.numPages
    print(pages_no)
    output = PdfFileWriter()
    for i in range(pages_no // 50):
        output.addPage(inputpdf.getPage(i * 50))
        if i * 50 + 1 < inputpdf.numPages:
            output.addPage(inputpdf.getPage(i * 50 + 1))
            print('/tmp/document-page%s.pdf' % i)
        newname = 'document-page%s.pdf' % i
        print(newname)
        with open("/tmp/document-page%s.pdf" % i, "wb") as outputStream:
            output.write(outputStream)
            client.upload_file('/tmp/'+newname, destbucketName,
                               'extracted-pdf/'+newname)


def lambda_handler(event, context):
    try:
        print(event)
        #myfile = event['Records'][0]['s3']['object']['key']
        myfile = 'CATASTROPHIC-DEVASTATION-OF-THE-ANCIENT-ONES.pdf'
        print(myfile)
        download_to_s3(myfile)
        split_pdf(myfile)

    except Exception as e:
        print(e)
