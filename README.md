SwanFlickrUploader
==================

Very Simple CommandLine Flickr Uploader for personal use


1. Get Flickr API Key, Secret
	http://www.flickr.com/services/apps/create/apply/

2. Get nsid
	http://www.flickr.com/services/api/explore/?method=flickr.people.getInfo

3. Run 
	java -jar SwanFlickrUploader.jar api_key secret nsid target_dir
	
	*target_dir must have 1 level of directories. A directory name become a set name.