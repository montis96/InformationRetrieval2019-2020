# Add the '<root>' to files

import os

count = 0
# all_files = os.listdir("D:\\Sgmon\\Documents\\Erasmus_Doc\\Corsi\\InformationRetrieval\\postsBig")

# for file in all_files:
# try:
content = open("D:\\Sgmon\\Documents\\Erasmus_Doc\\Corsi\\InformationRetrieval\\file1\\Q1041.xml",
               encoding="utf-8").read()
content = content.replace('</question>', '</question>\n<replies>')
final_substring = '</root>'
final_insert = '</replies>\n'
idx = content.index(final_substring)
content = content[:idx] + final_insert + content[idx:]
print(content)

with open("D:\\Sgmon\\Documents\\Erasmus_Doc\\Corsi\\InformationRetrieval\\file1\\Q1041.xml", "w+",
          encoding="utf-8") as text_file:
    print(content, file=text_file)

# except:
#     print(error)
