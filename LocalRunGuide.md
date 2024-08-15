
To run the application without Docker/Podman, you will need to manually install all dependencies and build the necessary components.

Note that some dependencies might not be available in the standard repositories of all Linux distributions, and may require additional steps to install.

The following guide assumes you have a basic understanding of using a command line interface in your operating system.

It should work on most Linux distributions and MacOS. For Windows, you might need to use Windows Subsystem for Linux (WSL) for certain steps.
The amount of dependencies is to actually reduce overall size, ie installing LibreOffice sub components rather than full LibreOffice package.

You could theoretically use a Distrobox/Toolbox, if your Distribution has old or not all Packages. But you might just as well use the Docker Container then.

### Step 1: Prerequisites

Install the following software, if not already installed:

- Java 17 or later

- Gradle 7.0 or later (included within repo so not needed on server)

- Git

- Python 3 (with pip)

- Make

- GCC/G++

- Automake

- Autoconf

- libtool

- pkg-config

- zlib1g-dev

- libleptonica-dev

For Debian-based systems, you can use the following command:

```bash
sudo apt-get update
sudo apt-get install -y git  automake  autoconf  libtool  libleptonica-dev  pkg-config zlib1g-dev make g++ java-17-openjdk python3 python3-pip
```

For Fedora-based systems use this command: 

```bash
sudo dnf install -y git automake autoconf libtool leptonica-devel pkg-config zlib-devel make gcc-c++ java-17-openjdk python3 python3-pip
```

### Step 2: Clone and Build jbig2enc (Only required for certain OCR functionality)

```bash
mkdir ~/.git
cd ~/.git &&\
git clone https://github.com/agl/jbig2enc.git &&\
cd jbig2enc &&\
./autogen.sh &&\
./configure &&\
make &&\
sudo make install
```

### Step 3: Install Additional Software
Next we need to install LibreOffice for conversions, ocrmypdf for OCR, and opencv for patern recognition functionality.

Install the following software:

- libreoffice-core

- libreoffice-common

- libreoffice-writer

- libreoffice-calc

- libreoffice-impress

- python3-uno

- unoconv

- pngquant

- unpaper

- ocrmypdf

- opencv-python-headless

For Debian-based systems, you can use the following command:

```bash
sudo apt-get install -y libreoffice-writer libreoffice-calc libreoffice-impress unpaper ocrmypdf
pip3 install uno opencv-python-headless unoconv pngquant 
```

For Fedora:

```bash
sudo dnf install -y libreoffice-writer libreoffice-calc libreoffice-impress unpaper ocrmypdf
pip3 install uno opencv-python-headless unoconv pngquant 
```

### Step 4: Clone and Build Stirling-PDF

```bash
cd ~/.git &&\
git clone https://github.com/Frooodle/Stirling-PDF.git &&\
cd Stirling-PDF &&\
chmod +x ./gradlew &&\
./gradlew build
```


### Step 5: Move jar to desired location

After the build process, a `.jar` file will be generated in the `build/libs` directory.
You can move this file to a desired location, for example, `/opt/Stirling-PDF/`.
You must also move the Script folder within the Stirling-PDF repo that you have downloaded to this directory.
This folder is required for the python scripts using OpenCV

```bash
sudo mkdir /opt/Stirling-PDF &&\
sudo mv ./build/libs/Stirling-PDF-*.jar /opt/Stirling-PDF/ &&\
sudo mv scripts /opt/Stirling-PDF/ &&\
echo "Scripts installed."
```
### Step 6: Other files
#### OCR
If you plan to use the OCR (Optical Character Recognition) functionality, you might need to install language packs for Tesseract if running non-english scanning.

##### Installing Language Packs
Easiest is to use the langpacks provided by your repositories. Skip the other steps

Manual:

1. Download the desired language pack(s) by selecting the `.traineddata` file(s) for the language(s) you need.
2. Place the `.traineddata` files in the Tesseract tessdata directory: `/usr/share/tesseract-ocr/5/tessdata`
3. 
Please view  [OCRmyPDF install guide](https://ocrmypdf.readthedocs.io/en/latest/installation.html) for more info.
**IMPORTANT:** DO NOT REMOVE EXISTING `eng.traineddata`, IT'S REQUIRED.

Debian based systems, install languages with this command:

```bash
sudo apt update &&\
# All languages
# sudo apt install -y 'tesseract-ocr-*'

# Find languages:
apt search tesseract-ocr-

# View installed languages:
dpkg-query -W tesseract-ocr- | sed 's/tesseract-ocr-//g'
```

Fedora:

```bash
# All languages
# sudo dnf install -y tesseract-langpack-*

# Find languages:
dnf search -C tesseract-langpack-

# View installed languages:
rpm -qa | grep tesseract-langpack | sed 's/tesseract-langpack-//g'
```

### Step 7: Run Stirling-PDF

```bash
./gradlew bootRun
or
java -jar build/libs/app.jar
```

### Step 8: Adding a Desktop icon

This will add a modified Appstarter to your Appmenu.
```bash
location=$(pwd)/gradlew
image=$(pwd)/docs/stirling-transparent.svg

cat > ~/.local/share/applications/Stirling-PDF.desktop <<EOF
[Desktop Entry]
Name=Stirling PDF;
GenericName=Launch StirlingPDF and open its WebGUI;
Category=Office;
Exec=xdg-open http://localhost:8080 && nohup $location bootRun &;
Icon=$image;
Keywords=pdf;
Type=Application;
NoDisplay=false;
Terminal=true;
EOF
```

Note: Currently the app will run in the background until manually closed.

---

Remember to set the necessary environment variables before running the project if you want to customize the application the list can be seen in the main readme.

You can do this in the terminal by using the `export` command or -D arguements to java -jar command:

```bash
export APP_HOME_NAME="Stirling PDF"
or
-DAPP_HOME_NAME="Stirling PDF" 
```
