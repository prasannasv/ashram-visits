package org.ishausa.registration.iii.payment;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * Created by Prasanna Venkat on 3/4/2017.
 */
public class CardOwnerInfo {
    private final String firstName;
    private final String lastName;
    private final String addressLine1;
    private final String city;
    private final String state;
    private final String zip;
    private final String country;
    private final String email;

    private CardOwnerInfo(final String firstName,
                          final String lastName,
                          final String addressLine1,
                          final String city,
                          final String state,
                          final String zip,
                          final String country,
                          final String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.addressLine1 = addressLine1;
        this.city = city;
        this.state = state;
        this.zip = zip;
        this.country = country;
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getZip() {
        return zip;
    }

    public String getCountry() {
        return country;
    }

    public String getEmail() {
        return email;
    }

    public static class Builder {
        private String firstName;
        private String lastName;
        private String addressLine1;
        private String city;
        private String state;
        private String zip;
        private String country = "US";
        private String email;

        public Builder withFirstName(final String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder withLastName(final String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder withAddressLine1(final String addressLine1) {
            this.addressLine1 = addressLine1;
            return this;
        }

        public Builder withCity(final String city) {
            this.city = city;
            return this;
        }

        public Builder withState(final String state) {
            this.state = state;
            return this;
        }

        public Builder withZip(final String zip) {
            this.zip = zip;
            return this;
        }

        public Builder withCountry(final String country) {
            this.country = country;
            return this;
        }

        public Builder withEmail(final String email) {
            this.email = email;
            return this;
        }

        public CardOwnerInfo build() {
            Preconditions.checkNotNull(Strings.emptyToNull(firstName), "First name must be set");
            Preconditions.checkNotNull(Strings.emptyToNull(lastName), "Last name must be set");
            Preconditions.checkNotNull(Strings.emptyToNull(addressLine1), "Address line must be set");
            Preconditions.checkNotNull(Strings.emptyToNull(city), "City must be set");
            Preconditions.checkNotNull(Strings.emptyToNull(state), "State must be set");
            Preconditions.checkNotNull(Strings.emptyToNull(zip), "Zipcode must be set");
            Preconditions.checkNotNull(Strings.emptyToNull(country), "Country must be set");
            Preconditions.checkNotNull(Strings.emptyToNull(email), "Email must be set");

            return new CardOwnerInfo(firstName, lastName, addressLine1, city, state, zip, country, email);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("firstName", firstName)
                .add("lastName", lastName)
                .add("addressLine1", addressLine1)
                .add("city", city)
                .add("state", state)
                .add("zip", zip)
                .add("country", country)
                .add("email", email)
                .toString();
    }
}
