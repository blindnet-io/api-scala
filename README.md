<h1 align="center">
  blindnet devkit<br />
  API v1
</h1>

<p align=center><img src="https://user-images.githubusercontent.com/7578400/163277439-edd00509-1d1b-4565-a0d3-49057ebeb92a.png#gh-light-mode-only" height="80" /></p>
<p align=center><img src="https://user-images.githubusercontent.com/7578400/163549893-117bbd70-b81a-47fd-8e1f-844911e48d68.png#gh-dark-mode-only" height="80" /></p>

<p align="center">
  <strong>Implementation of blindnet devkit API v1, in Scala</strong>
</p>

<p align="center">
  <a href="https://blindnet.dev"><strong>blindnet.dev</strong></a>
</p>

<p align="center">
  <a href="https://blindnet.dev/docs">Documentation</a>
  &nbsp;•&nbsp;
  <a href="https://github.com/blindnet-io/blindnet-backend-scala/issues">Submit an Issue</a>
  &nbsp;•&nbsp;
  <a href="https://join.slack.com/t/blindnet/shared_invite/zt-1arqlhqt3-A8dPYXLbrnqz1ZKsz6ItOg">Online Chat</a>
  <br>
  <br>
</p>

## Get Started

:rocket: Check out our [Quick Start Guide](https://blindnet.dev/docs/quickstart) to get started in a snap.

## Installation

Use [sbt](https://www.scala-sbt.org) to build the server JAR:

```bash
sbt assembly
```

The server JAR will be available at `target/scala-3.x.x/blindnet.jar`.

Optionally, build the docker image:

```bash
docker build .
```

## Usage

### Database (PostgreSQL)

This project uses a PostgreSQL database. Create one and then put the needed configuration options in the environment
variables below. Migrations run during startup of the server, if enabled (see options below). 

### Environment variables

Required if no default value.

| Name                       | Description                  | Example                                | Default       |
|----------------------------|------------------------------|----------------------------------------|:--------------|
| BN_ENV                     | Environment                  | development, staging, production       | development   |
| BN_MIGRATE                 | Enables DB migration         | yes, no                                | env-dependant |
| BN_DB_URI                  | Database URI                 | `jdbc:postgresql://localhost/blindnet` |               |
| BN_DB_USER                 | Database username            | `blindnet`                             |               |
| BN_DB_PASSWORD             | Database password            | `blindnet`                             |               |
| BN_PORT                    | HTTP port                    | 80                                     | 8087          |
| BN_HOST                    | HTTP host                    | 0.0.0.0                                | 127.0.0.1     |
| BN_AZURE_STORAGE_ACC_NAME  | Azure storage account name   |                                        |               |
| BN_AZURE_STORAGE_ACC_KEY   | Azure storage account key    |                                        |               |
| BN_AZURE_STORAGE_CONT_NAME | Azure storage container name |                                        |               |

## Contributing

Contributions of all kinds are always welcome!

If you see a bug or room for improvement in this project in particular, please [open an issue][new-issue] or directly [fork this repository][fork] to submit a Pull Request.

If you have any broader questions or suggestions, just open a simple informal [DevRel Request][request], and we'll make sure to quickly find the best solution for you.

## Community

> All community participation is subject to blindnet’s [Code of Conduct][coc].

Stay up to date with new releases and projects, learn more about how to protect your privacy and that of our users, and share projects and feedback with our team.

- [Join our Slack Workspace][chat] to chat with the blindnet community and team
- Follow us on [Twitter][twitter] to stay up to date with the latest news
- Check out our [Openness Framework][openness] and [Product Management][product] on Github to see how we operate and give us feedback.

## License

The blindnet devkit {short-project-name} is available under [MIT][license] (and [here](https://github.com/blindnet-io/openness-framework/blob/main/docs/decision-records/DR-0001-oss-license.md) is why).

<!-- project's URLs -->
[new-issue]: https://github.com/blindnet-io/blindnet-backend-scala/issues/new/choose
[fork]: https://github.com/blindnet-io/blindnet-backend-scala/fork

<!-- common URLs -->
[devkit]: https://github.com/blindnet-io/blindnet.dev
[openness]: https://github.com/blindnet-io/openness-framework
[product]: https://github.com/blindnet-io/product-management
[request]: https://github.com/blindnet-io/devrel-management/issues/new?assignees=noelmace&labels=request%2Ctriage&template=request.yml&title=%5BRequest%5D%3A+
[chat]: https://join.slack.com/t/blindnet/shared_invite/zt-1arqlhqt3-A8dPYXLbrnqz1ZKsz6ItOg
[twitter]: https://twitter.com/blindnet_io
[docs]: https://blindnet.dev/docs
[changelog]: CHANGELOG.md
[license]: LICENSE
[coc]: https://github.com/blindnet-io/openness-framework/blob/main/CODE_OF_CONDUCT.md
