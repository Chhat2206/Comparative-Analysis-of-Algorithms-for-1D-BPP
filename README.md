# Java Development Environment Setup Guide

This guide will help you set up your Java development environment using IntelliJ IDEA and the current version of the JDK and run "COMP2024-CW-Group18" project.

## Prerequisites

Before you begin, ensure that you have administrative access to your computer and an active internet connection to download the necessary software.

## 1. Install the Java Development Kit (JDK)

### Download JDK

- Visit the [Oracle JDK Downloads page](https://www.oracle.com/java/technologies/javase-downloads.html).
- Select the current version of the JDK for your operating system (Windows, macOS, Linux).
- Download the installer file.

### Install JDK

- Run the downloaded installer.
- Follow the installation prompts. Make a note of the JDK installation path, as you may need it later for configuration purposes.

### Set Environment Variables (Windows)

- Right-click on 'This PC' and select 'Properties'.
- Click on 'Advanced system settings' and then 'Environment Variables'.
- Under 'System Variables', find and select 'Path', then click 'Edit'.
- Add the path to the `bin` directory of your JDK installation (e.g., `C:\Program Files\Java\jdk-current\bin`).
- Create a new system variable:
    - Name: `JAVA_HOME`
    - Value: the path to your JDK installation (e.g., `C:\Program Files\Java\jdk-current`).
- Click 'OK' to close all dialogues.

## 2. Install IntelliJ IDEA

### Download IntelliJ IDEA

- Visit the [IntelliJ IDEA download page](https://www.jetbrains.com/idea/download/).
- Choose the edition you wish to install (Community or Ultimate).
- Download the installer for your operating system.

### Install IntelliJ IDEA

- Run the downloaded installer.
- Follow the installation prompts, accepting the default settings or customizing them as needed.

## 3. Configure IntelliJ IDEA

- **Open IntelliJ IDEA.**
    - When opening for the first time, you might be prompted to import settings. If you are a new user, select 'Do not import settings'.
- **Configure JDK:**
    - Go to `File` > `Project Structure` > `Project`.
    - Click on 'New...' next to the 'Project SDK' field.
    - Select 'JDK', then navigate to the directory where you installed the current version of the JDK (e.g., `C:\Program Files\Java\jdk-current`).
    - Click 'OK' to set the JDK for your projects.

## 4. Open Your Project Folder

- To open the project named "COMP2024-CW-Group18":
    - Navigate to `File` > `Open` in IntelliJ IDEA.
    - Browse to the directory containing the "COMP2024-CW-Group18" project.
    - Select the project directory and click 'OK'.

## Conclusion

You are now ready to run "COMP2024-CW-Group18" project.
