package uk.ac.ox.it.calendarimporter;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import uk.ac.ox.it.calendarimporter.service.DepositService;

/**
 * Starts a CLI for testing during development
 * - Supports testing of DepositService during development
 */
@Slf4j
@Component
@ConditionalOnProperty(value="dev.command-line.enabled", havingValue = "true")
public class DevelopmentCommandLineRunner implements CommandLineRunner {


    @Autowired
    private DepositService depositService;


    @Override
    public void run(String... runArgs) throws Exception {
        log.info("Starting dev CLI");
        Scanner scanner = new Scanner(System.in);


        boolean keepGoing = true;

        while (keepGoing) {
            String workingDir = Path.of("").toAbsolutePath().toString();
            System.out.print(workingDir + ">");
            String input = scanner.nextLine();


            if (input.isBlank()) continue;

            String[] inputParts = input.split("\\s+");

            String command = inputParts[0];
            List<String> args = new ArrayList<>(List.of(inputParts));
            args.remove(0);

            switch(command) {
                case "deposit":
                    if (args.size() != 2) {
                        log.error("Command 'deposit' must have 2 arguments");
                        log.info("Usage: deposit <type> <filepath>");
                        continue;
                    }

                    String typeArg = args.get(0);
                    DepositService.Type type = "log".equals(typeArg)
                            ? DepositService.Type.LOG
                            : "upl".equals(typeArg)
                                ? DepositService.Type.UPLOAD
                                : null;

                    if (type == null) {
                        log.error("Invalid type {}", typeArg);
                        continue;
                    }

                    Path sourcePath = Path.of(args.get(1));
                    File sourceFile = sourcePath.toFile();

                    if (!sourceFile.exists()) {
                        log.error("File '{}' does not exist", sourcePath.toAbsolutePath().toString());
                        continue;
                    }

                    log.info("Trying to deposit {}", sourcePath);

                    Path depositPath = depositService.deposit(sourceFile, type);

                    log.info("DepositService.deposit returned '{}'", depositPath);
                    break;
                case "remove":
                    if (args.size() != 1) {
                        log.error("Command 'remove' must have 1 argument");
                        log.info("Usage: remove <deposit>");
                        continue;
                    }

                    String deposit = args.get(0);

                    depositService.remove(deposit);

                    log.info("Removed '{}'", deposit);
                    break;
                case "exit":
                case "quit":
                    keepGoing = false;
                    break;
                default:
                    log.info("Unknown command. Type 'exit' to quit.");
                    break;
            }
        }

        scanner.close();
     }
}
