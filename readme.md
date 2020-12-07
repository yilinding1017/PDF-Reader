*Yilin Ding*<br>*20765311 y264ding*<br>*macOS 11.0.1 (MacBook Pro 2019)*<br>*openjdk version "11.0.8" 2020-07-14*<br>*Android SDK: minSdkVersion 26, targetSdkVersion 30*<br>*Sources used/refered:*<br>https://www.flaticon.com/<br>

### Application Description:

This **padreader** application will load a sample PDF "shannon1948.pdf" and the name of this pdf will be shown in the center of the titlebar at the top together with the **undo/redo** button each by its side. The statusbar at the very top shows the application name "pdfreader" at the left as well as an option menu at the right expanding to **Draw, Hilight, and Erase**. The pagebar at the buttom displays the current page number and the total number of pages (e.g. "page 2/5") at the center; the two ends "Previous Page" and "Next Page" of the pagebar are used to move between pages. 

##### **Functional Descriptions:**

- Annotatation Tools: there are three annotation tools as listed below, which can be found in the optionsmenu in the top statusbar
  - Draw: the user can write on the screen with a thin line in blue ink when the draw tool is selectedd; this is also the default mode if no option is selected.
  - Highlight: the user can draw over the existing document with a thick, transparent yellow brush that allows the user to highlight the text in the PDF when highlight tool is selected
  - Erase: the user can erase existing drawing/highlighting when the erase tool is selected
- Zoom & Pan
  - Zoom: the user can use two fingers to zoom-in and zoom-out over a focal point on the screen
  - Pan: the users can pan around to reposition the document using two fingers while not zooming
- Undo & Redo: the user can undo the last actions that were performed by clicking on Undo at the top left corner and they can redo to revert the undo operations by clicking on Redo at the top right corner. A **toast** of 2 seconds will be fired indicating what operations are being undo/redo when undo/redo tool is used.
- Pages: the first page of the pdf will be displayed when the app is launched and the user can do the followings with the pages
  - the user can move between pages by clicking on previous/next page button at the bottom left/right
  - when the user navigates between pages, the annotations tracked separated per page, and not lost

##### **Technical Descriptions:**

- Code built based on the starter code provided
- Used Android Studio to write all the code and do the test; might need to use Android Studio to run it if IntelliJ does not work
- Tested using Pixel X table with portrait orientation