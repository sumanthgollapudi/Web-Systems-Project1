# Project 1 – Facebook APP Proposal
```
Team: Group 8
Kishan (ir8803)
Sai Kumar (rl4901)
Parameswari (mm9827)
Venkata Sumanth Gollapudi (op9544)
```
Description:
It's a fun app Many times we are curious about what atmosphere suits best with the existing picture, with this Application we can suggest the vertices of a crop region on an picture with respect to the user uploaded photos or photos from Facebook. The uploaded photo is stored in google cloud storage and then by using the Cloud Vision API we can Detect image properties.
Workflow:
1.	User can upload photos from Facebook or from computer
2.	Then it will check whether the image is present in the cloud database. if it is not present then it will send to the Google cloud.
3.	Google Cloud Vision API will process that image and generate image properties(dominant colour).
4.	Result page displays the image with another suitable atmospheric image (depends on dominant colour).
