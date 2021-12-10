CREATE TABLE Caregivers (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Availabilities (
    Time DATE,
    Username varchar(255) REFERENCES Caregivers,
    PRIMARY KEY (Time, Username)
);

CREATE TABLE Vaccines (
    Name varchar(255),
    Doses int,
    PRIMARY KEY (Name)
);

CREATE TABLE Patient (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY(Username)
);

CREATE TABLE Appointments (
    ID int,
    Patient_name varchar(255) REFERENCES Patient(Username),
    Caregiver_name varchar(255) REFERENCES Caregivers(Username),
    Vaccine_name varchar(255) REFERENCES  Vaccines(Name),
    Time DATE,
    PRIMARY KEY (ID)
);