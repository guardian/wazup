# Wazup

Automated deployment of Wazuh configuration files
_____

[Wazuh](https://github.com/wazuh/wazuh) is an open source platform for threat detection, security monitoring and incident response. It can be used to monitor endpoints, cloud services and containers, and to aggregate and analyze data from external sources.

Wazup is intended to be run on every node in a [Wazuh cluster](https://documentation.wazuh.com/current/user-manual/configuring-cluster/basics.html) and handles the entire lifecycle of the Wazuh manager, including starting the service for the first time.

## Purpose

Wazup allows a single, centralised source of configuration that can be polled by each node in the cluster. Wazup polls the storage location of the configuration, customises the configuration depending on the node type and inserts missing secrets.

Additionally, the configuration can be kept under version control and follow a change management system whereby updates are reviewed and approved before they are applied to production.

Wazup removes the requirement for anyone to ssh onto an instance to change the configuration.

## Development

You will need to create a local configuration file; ideally in an alternative directory to minimise the risk of it being committed to the repository.

The configuration can use HOCON format and requires the following entries:

```
wazup {
  bucket = ???
  bucketPath = ???
  parameterPrefix = ???
  confPath = ???
}
```

Run Wazup with sbt and provide the path to the local configuration file:

    sbt -Dconfig.file=<PATH>/wazup.local.conf run

## Deployment

Wazup does not yet have automated builds or deployment.

The debian package can be created with the sbt-native-packager:

```
sbt debian:packageBin
```

Once the package has been built, use the AWS command line tools to upload it to S3:

```
aws s3 cp target/wazup.deb s3://$BUCKET_NAME/$STACK/$STAGE/ --profile $PROFILE
```
