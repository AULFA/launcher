# LFA Launcher

This is an Android app that functions as a launcher for the other applications on our Spark Kits. It can be set to be the default app launcher for an Android device and it will only show applications from a predefined whitelist.

This is an Android Studio project written in Kotlin.

## Available variants

This table lists all the currently available variants of the Launcher app with the following information:
- Name: the name we normally use to refer to a specific version of the Launcher. Each version can have a different app ID and whitelist.
- Description: a brief summary of the purpose of this version.
- App ID: the identifier used by Android to differentiate between the apps.
- Whitelist: the list of apps that are shown, if installed, by this Launcher version. As the list is occasionally updated, this is a link to the list of app IDs used by the latest build of this Launcher version.

| Name | App ID | Description | Whitelist |
| --- | --- | --- | --- |
| Launcher | au.org.libraryforall.launcher.app | The default Launcher version. Includes all of the LFA applications we ship with our kits, plus Bloom Reader. | [strings.xml](au.org.libraryforall.launcher.app/src/main/res/values/strings.xml) |
| Inclusiv Launcher | au.org.libraryforall.launcher.inclusiv | Shipped with the Western Province and other programs organised with Inclusiv Education. This is the Launcher version installed on the devices used by the students. It includes some LFA Android Reader versions, Bloom Reader and other education apps. | [strings.xml](au.org.libraryforall.launcher.inclusiv/src/main/res/values/strings.xml) |
| Inclusiv Teacher Launcher | au.org.libraryforall.launcher.inclusiv_teacher | Shipped with the Western Province and other programs organised with Inclusiv Education. This is the Launcher version installed on the devices used by the teachers. It includes some LFA Android Reader versions, Bloom Reader, education apps and other utilities. | [strings.xml](au.org.libraryforall.launcher.inclusiv_teacher/src/main/res/values/strings.xml) |
| LFA Launcher Ukraine | au.org.libraryforall.launcher.ukraine | Shipped with the Ukraine program. It’s currently translated to Ukrainian using Google Translate. Includes several third-party apps in the whitelist. | [strings.xml](au.org.libraryforall.launcher.ukraine/src/main/res/values/strings.xml) |

## Setup
1. Clone the repo:
```bash
git clone git@github.com:AULFA/launcher.git
```
2. Get the submodules:
```bash
cd launcher
git submodule update --init
```
3. Clone the application-secrets directory into the `.ci` directory, naming it `credentials`:
```bash
cd .ci
git clone git@github.com:AULFA/application-secrets.git credentials
```
4. Run the `credentials.sh` script to set up the required credential files for the app:
```bash
cd ..
.ci-local/credentials.sh
```

Done! You should be able to build the project in any of the available variants now.

## How-tos

### Increment the version number
The version of the Launcher app is composed by two parts: version name and version code.

#### Version code
The version code of a specific variant of the app is provided by the `version.properties` file of the variant:
```bash
#
#Tue Aug 23 14:16:09 AEST 2022
VERSION_CODE=370

```
This file is updated automatically every time you (or Android Studio) rebuild the app, so you shouldn't need to update it manually.
If you need to change it, it's better if you rebuild the app and commit the new content of this file. You can change it manually too, just make sure that the new version code is higher than the previous one.

#### Version name
The version name of the app is defined under the `gradle.properties` file of the project:
```bash
VERSION_NAME=0.0.5-SNAPSHOT
```
You can update this by simply incrementing the version in this variable.

### Create a new version of the Launcher
You might need to create a new variant of the app to provide a different translation or whitelist for a specific program. For example, the Ukraine program has a separate variant of the Launcher with a different whitelist and translation.
If you want to create a new variant of the Launcher, you can follow these steps:
1. Duplicate the whole `<project root>/au.org.libraryforall.launcher.app` directory. Rename it to `au.org.libraryforall.launcher.<name of the program>`.
2. Update the `settings.gradle` file of the project to include the new package.
3. Locate the `AndroidManifest.xml` file of your variant. Change the app's title and package name according to your preference. The package name should ideally be the same as the package directory name (`au.org.libraryforall.launcher.<name of the program>`).
4. Locate the `res/values/strings.xml` file of your variant. Change the whitelist and translation according to your needs.
5. Update the `.ci-local/deploy-ssh.conf` file of the project to include the new variant.

You should now be able to run, test and release the new variant.

## Release
Once you're done with your changes, you may want to release a new version of the app. To do this, follow these steps:
1. Commit your changes to a new branch and push them to the GitHub repository.
2. Create a pull request detailing the changes you made (features, fixes, new versions...). The PR should point to the `develop` branch.
3. Depending on the changes made and the availability of the rest of the team, ask for a code review.
4. Once everything is ready, squash and merge the pull request.
5. A GitHub Action will start. This will build all the app variants and, if successful, push the generated APK files automatically to the testing repository.
6. You will find the latest version of each variant in the testing repository https://distribution.lfa.one/repository/testing/

## Memento
![launcher](./src/site/resources/launcher.jpg?raw=true)

