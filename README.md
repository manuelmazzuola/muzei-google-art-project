# Muzei Google Art Project
**Muzei Google Art Project Source** is an artwork source for [muzei](http://muzei.co) that displays random pictures from [Google Art Project](https://www.google.com/culturalinstitute/u/0/project/art-project?hl=it).

![Icon](http://i.picresize.com/images/2015/03/25/xDnmC.png)


## How
*Google Art Project* does not provide an `API` so I've reverse engineered the [Google Art Project Chrome Extension](https://chrome.google.com/webstore/detail/google-art-project/akimgimeeoiognljlfchpbkpfbmeapkh).

I've discovered that the extension picks a random masterpiece from a [json file](https://github.com/manuelmazzuola/muzei-google-art-project/blob/master/app/src/main/assets/imax.json) that contains an array of objects. Each object contains the masterpiece's informations like the title, author, image url, etc.

The only problem I had was with the resolution of the images. Each url in the json file links to a low resolution image, but the `Google Art Project` extension displays a high resolution image.

So, with the help of [Wireshark](https://www.wireshark.org/about.html) I've discovered that the `GAP` extension appends the string `=1200-rw` to each url, in this way the url points to an image at most large 1200px.


## Attributions
This app is based upon *Roman Nurik*'s *Muzei sample source app* and uses the Muzei API.
[Muzei](http://muzei.co) is released under the *Apache 2.0* license.

See the LICENSES file for further details.
