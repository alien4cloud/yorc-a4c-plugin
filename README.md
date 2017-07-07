# Alien4Cloud Janus Plugin

Janus plugin allows users to deploy their application in an HPC environment.
This means that Janus will allocate nodes with Slurm, then deploy component (docker image, for example) on the allocated node.

[Check Build Status](http://129.184.11.224/view/Janus%20A4C%20Plugin/)

## Requirements

[Alien4Cloud version 1.3.x](http://alien4cloud.github.io/#/documentation/1.3.0/index.html)


## Installation
### Alien4Cloud
* Download: [alien4Cloud v1.3 Download button](https://alien4cloud.github.io/)
* Run : alien4cloud.sh or alien4cloud.bat
* Visit [a4c UI: login admin/admin](http://localhost:8088)

### Plugin Janus
In the folder of our plugin : alien4cloud-janus-plugin in the folder target we can find the zipped folder alien4cloud-janus-plugin-<version>
Then go to administration tab -> plugins and here drag and drop your compressed folder.


## Quick Start
With this plugin we have a class called JanusPaaSProvider which contains the method doDeploy.
In this method we can have access to the Topology bean.
