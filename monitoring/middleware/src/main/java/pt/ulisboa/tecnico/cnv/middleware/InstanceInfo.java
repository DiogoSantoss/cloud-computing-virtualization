package pt.ulisboa.tecnico.cnv.middleware;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.ec2.model.Instance;

public class InstanceInfo {
    
    private Instance instance;
    private List<Request> requests;
    private int missedHealthChecks;

    public InstanceInfo(Instance instance) {
        this.instance = instance;
        this.requests = new ArrayList<>();
        this.missedHealthChecks = 0;
    }

    public Instance getInstance() {
        return this.instance;
    }

    public int getMissedHealthChecks() {
        return this.missedHealthChecks;
    }

    public List<Request> getRequests() {
        return this.requests;
    }

    public void incrementMissedHealthChecks() {
        this.missedHealthChecks++;
    }

    public void resetMissedHealthChecks() {
        this.missedHealthChecks = 0;
    }

    public double getLoad() {
        return this.requests.stream().mapToDouble(r -> r.getEstimatedCost()).sum();
    }
}
