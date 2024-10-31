import os
import sys
from openai import OpenAI
import pprint
import json

# Set up OpenAI API key from environment variable

# Read data from stdin
data = sys.stdin.read()

# Create the prompt with data
prompt = """
I'll provide text from the clipboard; it will be random text that I've highlighted and copied. Your task is to guess what this text is about and construct a mind map representation in the following JSON format:

json nodename is a name of mindmap node 
if nodename has child, then this is a child of parent in mindmap node
if nodename has key "detail" put some details about nodename
if nodename has key "link" then the link will be attached to mindmap node
if nodename has key not equal detail, then is a key = value argument, here is a example mindmap

{
    "house": {
        "temperature": "38",
        "detail": "house address",
        "aparments": {
            "room420": {}, 
            "room308": {}
        } 
    }
} 
In answer generate only code snippet without any additional explanation and text
My clipboard data is:
""" + data

# Send prompt to OpenAI and get response
client = OpenAI()
#openai.api_key = os.getenv("OPENAI_API_KEY")
completion = client.chat.completions.create(
    model="gpt-4o",
    messages=[
        {"role": "user", "content": prompt}
    ]
)

# Print the result
answer=completion.choices[0].message.content
# Split the answer into lines and remove the first and last lines
lines = answer.splitlines()[1:-1]

# Join the remaining lines back into a single string
json_string = "\n".join(lines)

# Parse the string as JSON
parsed_json = json.loads(json_string)

print(json_string)
