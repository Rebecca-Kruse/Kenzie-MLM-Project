package com.kenzie.marketing.application.service;

import com.kenzie.marketing.application.controller.model.CreateCustomerRequest;
import com.kenzie.marketing.application.controller.model.CustomerResponse;
import com.kenzie.marketing.application.controller.model.LeaderboardUiEntry;
import com.kenzie.marketing.application.repositories.CustomerRepository;
import com.kenzie.marketing.application.repositories.model.CustomerRecord;
import com.kenzie.marketing.referral.model.CustomerReferrals;
import com.kenzie.marketing.referral.model.LeaderboardEntry;
import com.kenzie.marketing.referral.model.Referral;
import com.kenzie.marketing.referral.model.ReferralRequest;
import com.kenzie.marketing.referral.model.client.ReferralServiceClient;
import org.joda.time.DateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.UUID.randomUUID;

@Service
public class CustomerService {
    private static final Double REFERRAL_BONUS_FIRST_LEVEL = 10.0;
    private static final Double REFERRAL_BONUS_SECOND_LEVEL = 3.0;
    private static final Double REFERRAL_BONUS_THIRD_LEVEL = 1.0;

    private CustomerRepository customerRepository;
    private ReferralServiceClient referralServiceClient;

    public CustomerService(CustomerRepository customerRepository, ReferralServiceClient referralServiceClient) {
        this.customerRepository = customerRepository;
        this.referralServiceClient = referralServiceClient;
    }

    /**
     * findAllCustomers
     * @return A list of Customers
     */
    public List<CustomerResponse> findAllCustomers() {
        List<CustomerRecord> records = StreamSupport.stream(customerRepository.findAll().spliterator(), true).collect(Collectors.toList());

        // Task 1 - Add your code here

        return records.stream()
                .map(this::toCustomerResponseFromRecord)
                .collect(Collectors.toList());
    }

    /**
     * findByCustomerId
     * @param customerId
     * @return The Customer with the given customerId
     */
    public CustomerResponse getCustomer(String customerId) {

        // Task 1 - Add your code here

        return toCustomerResponseFromRecord(customerRepository.findById(customerId).orElse(null));
    }

    /**
     * addNewCustomer
     *
     * This creates a new customer.  If the referrerId is included, the referrerId must be valid and have a
     * corresponding customer in the DB.  This posts the referrals to the referral service
     * @param createCustomerRequest
     * @return A CustomerResponse describing the customer
     */

    //You'll use Optional<> types to create your solution - remember the methods isPresent(), get(), or even orElseThrow().
    public CustomerResponse addNewCustomer(CreateCustomerRequest createCustomerRequest) {

//      1.  There are two options for the referrerId - it's either empty (meaning the customer joined by themselves) or
//        contains a value (meaning another customer with that ID referred them). If the request contains a
//        referrerId, then that must be a valid customer id from the table. If that referrerId does not exist in
//        the Customer table, the request should be rejected.
        CustomerRecord customerRecord = toCustomerRecord(createCustomerRequest);
        if(customerRecord.getReferrerId() != null && !customerRecord.getReferrerId().equals("") && !customerRepository.existsById(customerRecord.getReferrerId())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "referralID was not found");
        }

        ReferralRequest referralRequest = new ReferralRequest(customerRecord.getId(), customerRecord.getReferrerId());
        customerRepository.save(customerRecord);
        referralServiceClient.addReferral(referralRequest);

        return toCustomerResponseFromRecord(customerRecord);

//        2. The CustomerRecord should be created and saved into the Customer table. To create the customer ID, use a
//        call to record.setId(randomUUID().toString()).


//          3. A call should be made to referralServiceClient.addReferral() to add the new customer. It is important
//        that the referralServiceClient.addReferral() method is called for every customer added, even if they were
//        not referred! If a customer had no referrer, you should still call addReferral() using a blank referrerId.

//        4. A response object should be created and returned with all the necessary information.

    }

    /**
     * updateCustomer - This updates the customer name for the given customer id
     * @param customerId - The Id of the customer to update
     * @param customerName - The new name for the customer
     */
    public CustomerResponse updateCustomer(String customerId, String customerName) {
        Optional<CustomerRecord> customerExists = customerRepository.findById(customerId);
        if (customerExists.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer Not Found");
        }
        CustomerRecord customerRecord = customerExists.get();
        customerRecord.setName(customerName);
        customerRepository.save(customerRecord);

        // Task 1 - Add your code here

        return toCustomerResponseFromRecord(customerRecord);
    }

    /**
     * deleteCustomer - This deletes the customer record for the given customer id
     * @param customerId
     */
    public void deleteCustomer(String customerId) {
        customerRepository.deleteById(customerId);
    }

    /**
     * calculateBonus - This calculates the referral bonus for the given customer according to the referral bonus
     * constants.
     * @param customerId
     * @return
     */
    public Double calculateBonus(String customerId) {
        CustomerReferrals referrals = referralServiceClient.getReferralSummary(customerId);

        Double calculationResult = REFERRAL_BONUS_FIRST_LEVEL * referrals.getNumFirstLevelReferrals() +
                REFERRAL_BONUS_SECOND_LEVEL * referrals.getNumSecondLevelReferrals() +
                REFERRAL_BONUS_THIRD_LEVEL * referrals.getNumThirdLevelReferrals();

        return calculationResult;
    }

    /**
     * getReferrals - This returns a list of referral entries for every customer directly referred by the given
     * customerId.
     * @param customerId
     * @return
     */
    public List<CustomerResponse> getReferrals(String customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new IllegalArgumentException("Customer does not exist");
        }

        // Task 1 - Add your code here

        return Optional.ofNullable(referralServiceClient.getDirectReferrals(customerId))
                .orElse(Collections.emptyList())
                .stream()
                .map(this::toCustomerResponseFromReferral)
                .collect(Collectors.toList());
    }

    /**
     * getLeaderboard - This calls the referral service to retrieve the current top 5 leaderboard of the most referrals
     * @return
     */
    public List<LeaderboardUiEntry> getLeaderboard() {
          List<LeaderboardEntry> leaderboardList = referralServiceClient.getLeaderboard();

        // Task 2 - Add your code here
        return leaderboardList.stream()
                .filter(leaderboardEntry -> leaderboardEntry.getNumReferrals() > 0)
                .map(this::toLeaderboardUI)
                .collect(Collectors.toList());
    }

    /* -----------------------------------------------------------------------------------------------------------
        Private Methods
       ----------------------------------------------------------------------------------------------------------- */

    // Add any private methods here

    private CustomerResponse toCustomerResponseFromRecord(CustomerRecord record) {
        if(record == null) {
            return null;
        }
        CustomerResponse customerResponse = new CustomerResponse();

        customerResponse.setId(record.getId());
        customerResponse.setName(record.getName());
        customerResponse.setDateJoined(record.getDateCreated());
        if (record.getReferrerId() != null && !record.getReferrerId().equals("")) {
            Optional<CustomerRecord> customerRecord = customerRepository.findById(record.getReferrerId());
            customerRecord.ifPresent(customerRecord1 -> customerResponse.setReferrerName(customerRecord1.getName()));
        }
        customerResponse.setReferrerId(record.getReferrerId());

        return customerResponse;
    }

    private CustomerRecord toCustomerRecord(CreateCustomerRequest createCustomerRequest) {
        CustomerRecord customerRecord = new CustomerRecord();
        customerRecord.setName(createCustomerRequest.getName());
        customerRecord.setId(randomUUID().toString());
        customerRecord.setDateCreated(DateTime.now().toString());
        customerRecord.setReferrerId(createCustomerRequest.getReferrerId().orElse(""));

        return customerRecord;
    }

    private CustomerResponse toCustomerResponseFromReferral (Referral referral){
        Optional<CustomerRecord> record = customerRepository.findById(referral.getCustomerId());
        if(record.isPresent()) {
            return toCustomerResponseFromRecord(record.get());
        }
        return null;
    }
    private LeaderboardUiEntry toLeaderboardUI(LeaderboardEntry entry) {
        LeaderboardUiEntry leaderboardUiEntry = new LeaderboardUiEntry();
        leaderboardUiEntry.setCustomerId(entry.getCustomerId());
        leaderboardUiEntry.setNumReferrals(entry.getNumReferrals());

        if (customerRepository.existsById(entry.getCustomerId())) {
            leaderboardUiEntry.setCustomerName(getCustomer(entry.getCustomerId()).getName());
        } else {
            leaderboardUiEntry.setCustomerName("No name present");
        }

        return leaderboardUiEntry;
    }
}
