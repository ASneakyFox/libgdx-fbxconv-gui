libgdx-fbxconv-gui
==================

Crossplatform Gui Wrapper for fbx-conv written in Java. Meant to be small and lightweight.

Also serves as a model preveiw utility.

Can convert files individually or in a batch.


Basic Usage
==================

You need Java 7 installed to use this software. It might work with Java 8, havent tried.

run fbxconv-gui.jar by double click on it to run it.

The first thing you need to do is set the path to your copy of fbx-conv

Click the Browse button in the Configuration tab and search for it. If I am allowed I may package this program with fbx-conv in the future and have it automatically detect. 

Until then you can download fbx-conv from: https://github.com/libgdx/fbx-conv . 

fbxconv-gui was tested with the latest release version of fbx-conv available on 10/30/2014, other versions may or may not work

If youre having problems check to make sure you can use the command line tool by itself, check its readme file on github for more info.


Previewing 3D Models
==================

Use the file list on the left to search for 3d models to preview (supports .obj, .fbx, .dae, .g3db, .g3dj)

Models in the format of .obj, .fbx, or .dae will be converted to .g3dj for previewing your specified configuration options on the bottom left. .g3dj and .g3db files do not be converted and will be viewed directly.

If you have "Automatic Preview" checked then model files will automatically be shown when you select them, otherwise you need to press the "Preview" button.

If you make change in the Configuration tab you need to press "Preview" again to see the changes, even if Automatic Preview is turned on.

Previeiwng files is not the same as converting, You must push the "Convert" button in the Configuration tab to convert and save the current file.


Single File and Batch Convert
==================

When you drag and drop a directory, or multiple files and directories in to the window, it will do a batch convert. (a dialog will appear)

It will ask you what extension to use for the "source" file (eg .fbx). Tt will only convert files with that extension. The new file will have the same name and be in the same directory, but with the new extension (either g3db or g3dj)

If you drag and drop a single file it will automatically convert it as if it was a batch convert, unless you have that option deselected on the left toolbox, then it will just preview it.


License
==================

Copyright (c) 2014, Daniel Strong

All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


