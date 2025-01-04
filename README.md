# **Yggdrasil**

<img src="https://i.imghippo.com/files/58gOT1723056343.jpg" alt="Yggdrasil-VCS" width="150" align="right">

![GitHub](https://img.shields.io/github/commit-activity/m/pratyush103/yggdrasil-vcs)
![GitHub](https://img.shields.io/github/contributors/pratyush103/yggdrasil-vcs?style=flat)

Yggdrasil is a version control system designed to be simple and easy to use. It is a command-line tool that allows you to track changes in your project and manage different versions of your code.

## Features

- **Simple Command-Line Interface**: Easy to use commands to manage your project versions.
- **Version Tracking**: Keep track of changes and revert to previous versions if needed.
- **Branching**: Manage different versions of your project simultaneously.

## Getting Started

### Prerequisites

- Java Development Kit (JDK) installed on your machine.

### Installation

1. Clone the repository:
   ```shell
   git clone https://github.com/pratyush103/yggdrasil-vcs.git
   ```
2. Navigate to the project directory:
   ```shell
   cd yggdrasil-vcs
   ```
3. Compile the project:
   ```shell
   javac -d bin src/*.java
   ```

### Usage

1. Initialize a new repository:
   ```shell
   ygg init
   ```
2. Add files to the repository:
   ```shell
   ygg add <file_name>
   ```
3. Commit changes:
   ```shell
   ygg commit -m "Your commit message"
   ```
4. View the commit history:
   ```shell
   ygg log
   ```
For compiled binary use `java -cp bin Main` instead of `ygg`
## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository.
2. Create a new branch (`git checkout -b feature-branch`).
3. Make your changes.
4. Commit your changes (`git commit -m 'Add some feature'`).
5. Push to the branch (`git push origin feature-branch`).
6. Open a pull request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgements

- Thanks to all the contributors who have helped in building this project.

For more information, visit the [repository](https://github.com/pratyush103/yggdrasil-vcs).

---

Feel free to adjust any sections or add more details as necessary.
