#!/usr/bin/env python
import os
import sys
import time
import zipfile

appdir = "./gwen-workspace"

dir = os.path.dirname(appdir)

if (not os.path.exists(appdir)):
	print "Error: " + appdir + " does not exist."
	sys.exit()

os.chdir(dir)

appdir = os.path.basename(appdir)

zipname = "target/workspace/gwen-workspace.zip"

searchname = appdir + "gwen"

zip = zipfile.ZipFile(zipname, 'w', zipfile.ZIP_DEFLATED)

for root, dirs, files in os.walk(appdir):
	for file in files:
		filename = os.path.join(root, file)

		if (filename[0:len(searchname)] == searchname):
			f = open(filename, 'r')
			bytes = f.read()
			f.close()

			info = zipfile.ZipInfo(filename)
			info.date_time = time.localtime()
			info.external_attr = 0100755 << 16L

			zip.writestr(info, bytes, zipfile.ZIP_DEFLATED)
		else:
			zip.write(filename)

zip.close()

print "Created " + zipname
