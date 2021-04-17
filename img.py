from urllib.request import urlopen
from wand.image import Image
from io import BytesIO
import boto3


bucket_name = 'mysplitbucket'

resource = urlopen(key)
with Image(file=resource) as document:
    for page_number, page in enumerate(document.sequence):
        with Image(page) as img:
            bytes_io_file = BytesIO(img.make_blob('JPEG'))
            filename = 'output_{0}.jpg'.format(page_number)
            s3.upload_fileobj(bytes_io_file, bucket_name, filename)
            
def lambda_handler(event, context):
    bucketName = event['Records'][0]['s3']['bucket']['name']
    key = urllib.parse.unquote_plus(event['Records'][0]['s3']['object']['key'], encoding='utf-8')
    try:
        pdf_to_image(key)
        print("Generated")
        
    except Exception as e:
        print(e)