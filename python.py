import urllib3
import boto3
import json
import urllib.parse
import os
import uuid

client = boto3.client('stepfunctions')


def run_ecs_task():
    # INPUT -> { (unique)"TransactionID": "foo", "Type":"PURCHASE"}
    try:
        response = client.start_execution(
            
            stateMachineArn=os.environ['step'],
            name=str(uuid.uuid1()),
            input='{\"Comment\" : \"insert your json here\"}',
        )
    except Exception as e:
        print(e)


def fetch_transaction_id():
    try:
        http = urllib3.PoolManager()
        
        uri = os.environ['uri']
        json_data = http.request('GET', uri).data
        print(json_data)
        result = json.loads(json_data)
        res = result['Transaction_ID']
        return res

    except Exception as e:
        print(e)


def upload_dynamodb(res):
    try:
        Transaction_ID = res
        dynamodb = boto3.resource('dynamodb')
        table = dynamodb.Table('Transaction_Table')
        table.put_item(
            Item={
                'Transaction_ID': Transaction_ID,
            }
        )
    except Exception as e:
        print(e)


def lambda_handler(event, context):
    try:
        run_ecs_task()
        res = fetch_transaction_id()
        upload_dynamodb(res)
    except Exception as e:
        print(e)
