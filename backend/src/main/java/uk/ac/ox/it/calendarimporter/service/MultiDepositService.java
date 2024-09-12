package uk.ac.ox.it.calendarimporter.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Implementation of DepositService that calls other services. The lowest ordered available implementation is 
 * used for uploads. For other operations we check to see if the implementation can handle the URI in the order that
 * we have them and call the first available implementation that claims the URI.
 */
@Service
@Primary // This is so that we inject this service.
public class MultiDepositService implements DepositService {
    
    private final Logger log = LoggerFactory.getLogger(MultiDepositService.class);
   
    @Autowired
    @Qualifier("implementation") // this prevents us picking ourselves as we don't want stackoverlows.
    private List<DepositService> depositServiceList;
    
    @PostConstruct
    public void init() {
        if (depositServiceList.isEmpty()) { // Better to fail early
            throw new RuntimeException("No deposit service implementation found.");
        }
        log.info("Configured deposit services: {}", String.join(", ", depositServiceList.stream().map(Object::toString).toList()));
    }
    
    
    @Override
    public String deposit(File file, Type type) throws IOException {
        // Deposits always go to the first 
        return depositServiceList.get(0).deposit(file, type);
    }

    @Override
    public InputStream getInputStream(String deposit, Map<String, String> parameters) throws IOException {
        DepositService depositService = getDepositService(deposit);
        return depositService.getInputStream(deposit, parameters);
    }

    @Override
    public void remove(String deposit) {
        DepositService depositService = getDepositService(deposit);
        depositService.remove(deposit);
    }

    private DepositService getDepositService(String deposit) {
        return depositServiceList
                .stream()
                .filter(service -> service.canHandle(deposit))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No service available to handle "+ deposit));
    }

}
