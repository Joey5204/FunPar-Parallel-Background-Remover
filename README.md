<h1>Parallel Background Remover in Scala</h1>
<h2>Overview</h2>
The objective of this project is to implement parallelism to increase the speed of processing images. 
This project uses Scala to leverage parallel processing techniques, specifically focusing on the GrabCut algorithm for image segmentation.
<br />

<h2>Features</h2>
Parallel Processing: Utilizes Scala’s future to process multiple image segments simultaneously. <br />
GrabCut Algorithm: Implements the GrabCut algorithm for accurate background removal in images. <br />

<h2>Project Structure</h2>
<code>src/main/scala/Main.scala</code>: The entry point of the application, containing the main logic for loading, processing, and saving images. <br />
<code>src/main/resources/</code>: Directory for storing input images. <br />
<code>output/</code>: Directory where processed images (with backgrounds removed) are saved. <br />
<code>build.sbt</code>: Configuration file for SBT, including dependencies and project settings. <br />

<h2>How to run this code: </h2>
1. Choose an image by adding an image to the resource folder <br />
2. Change imagePath inside of <code>Main.scala</code> to match your image <br />
3. Run using <code>sbt run</code> <br />
<h2>Summary of results</h2>
Parallel runtime: 20 seconds <br />
One grabcut operation: ~2 seconds
