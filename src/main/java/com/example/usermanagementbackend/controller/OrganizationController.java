    package com.example.usermanagementbackend.controller;

    import com.example.usermanagementbackend.entity.Organization;
    import com.example.usermanagementbackend.repository.OrganizationRepository;
    import lombok.RequiredArgsConstructor;
    import org.springframework.batch.core.*;
    import org.springframework.batch.core.launch.JobLauncher;
    import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
    import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
    import org.springframework.batch.core.repository.JobRestartException;
    import org.springframework.web.bind.annotation.PostMapping;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RestController;
    import java.io.InputStreamReader;
    import java.util.List;
    import java.util.Map;
    import java.util.HashMap;
    import java.util.List;

    @RestController
    public class OrganizationController {

        private final JobLauncher jobLauncher;
        // To launch the batch job
        private final Job job;  // The job to be executed
        private final OrganizationRepository organizationRepository;

        public OrganizationController(JobLauncher jobLauncher, Job job, OrganizationRepository organizationRepository) {
            this.jobLauncher = jobLauncher;
            this.job = job;
            this.organizationRepository = organizationRepository;
        }

        // Endpoint to trigger the CSV import job
        @PostMapping("/import")
        public Map<String, Object> importOrganizations() {
            Map<String, Object> response = new HashMap<>();
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("startAt", System.currentTimeMillis()) // Add a unique parameter to trigger the job
                    .toJobParameters();
            List<Organization> organizations = organizationRepository.findAll();
            response.put("processedOrganizations", organizations);
            try {
                JobExecution jobExecution = jobLauncher.run(job, jobParameters); // Run the job with parameters
            } catch (JobExecutionAlreadyRunningException | JobRestartException |
                     JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
                e.printStackTrace();
                response.put("error", "Batch job error: ");  // Return job status

            }
            return response;

        }
    }
