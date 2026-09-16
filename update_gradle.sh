#!/bin/bash
sed -i '/packageName = "pilcrow"/a \
            modules("java.instrument", "jdk.unsupported")\
            linux {\
                iconFile.set(project.file("src/main/resources/icon.png"))\
                appCategory = "Office"\
            }\
            macOS {\
                iconFile.set(project.file("src/main/resources/icon.png"))\
            }\
            windows {\
                iconFile.set(project.file("src/main/resources/icon.png"))\
            }' /home/abuhamza/pilcrow-linux/desktop-app/build.gradle.kts
