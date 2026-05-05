-- MySQL dump 10.13  Distrib 8.0.45, for macos15 (arm64)
--
-- Host: 127.0.0.1    Database: team7
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `Bookings`
--

DROP TABLE IF EXISTS `Bookings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Bookings` (
  `Booking_ID` int NOT NULL AUTO_INCREMENT,
  `User_ID` int NOT NULL,
  `Ride_ID` int DEFAULT NULL,
  `Origin` varchar(100) DEFAULT NULL,
  `Destination` varchar(100) DEFAULT NULL,
  `Departure_Date` datetime DEFAULT NULL,
  `Seats_Requested` int DEFAULT NULL,
  `Booking_Timestamp` datetime DEFAULT CURRENT_TIMESTAMP,
  `Status` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`Booking_ID`),
  KEY `fk_bookings_ride` (`Ride_ID`),
  KEY `fk_bookings_user` (`User_ID`),
  CONSTRAINT `fk_bookings_ride` FOREIGN KEY (`Ride_ID`) REFERENCES `Rides` (`Ride_ID`),
  CONSTRAINT `fk_bookings_user` FOREIGN KEY (`User_ID`) REFERENCES `Users` (`User_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Drivers`
--

DROP TABLE IF EXISTS `Drivers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Drivers` (
  `User_ID` int NOT NULL,
  `License_Number` varchar(50) DEFAULT NULL,
  `Verification_Status` varchar(20) DEFAULT NULL,
  `Driver_Rating` decimal(2,1) DEFAULT NULL,
  PRIMARY KEY (`User_ID`),
  CONSTRAINT `drivers_ibfk_1` FOREIGN KEY (`User_ID`) REFERENCES `Users` (`User_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Logs`
--

DROP TABLE IF EXISTS `Logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Logs` (
  `Log_ID` int NOT NULL AUTO_INCREMENT,
  `User_ID` int DEFAULT NULL,
  `Ride_ID` int DEFAULT NULL,
  `Booking_ID` int DEFAULT NULL,
  `Action_Type` enum('LOGIN','LOGOUT','PASSWORD_CHANGE','BOOKING_CREATED','BOOKING_CANCELLED','ACCOUNT_CREATED','ACCOUNT_DELETED','RIDE_CREATED','RIDE_CANCELLED') NOT NULL,
  `Log_Timestamp` datetime DEFAULT CURRENT_TIMESTAMP,
  `Description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`Log_ID`),
  KEY `User_ID` (`User_ID`),
  KEY `Ride_ID` (`Ride_ID`),
  KEY `Booking_ID` (`Booking_ID`),
  CONSTRAINT `logs_ibfk_1` FOREIGN KEY (`User_ID`) REFERENCES `Users` (`User_ID`),
  CONSTRAINT `logs_ibfk_2` FOREIGN KEY (`Ride_ID`) REFERENCES `Rides` (`Ride_ID`),
  CONSTRAINT `logs_ibfk_3` FOREIGN KEY (`Booking_ID`) REFERENCES `Bookings` (`Booking_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Notifications`
--

DROP TABLE IF EXISTS `Notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Notifications` (
  `Notification_ID` int NOT NULL AUTO_INCREMENT,
  `User_ID` int NOT NULL,
  `Content` text,
  `Timestamp` datetime DEFAULT NULL,
  `Read_Status` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`Notification_ID`),
  KEY `fk_notifications_user` (`User_ID`),
  CONSTRAINT `fk_notifications_user` FOREIGN KEY (`User_ID`) REFERENCES `Users` (`User_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Passengers`
--

DROP TABLE IF EXISTS `Passengers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Passengers` (
  `User_ID` int NOT NULL,
  `Total_Rides_Taken` int DEFAULT NULL,
  PRIMARY KEY (`User_ID`),
  CONSTRAINT `passengers_ibfk_1` FOREIGN KEY (`User_ID`) REFERENCES `Users` (`User_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Reviews`
--

DROP TABLE IF EXISTS `Reviews`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Reviews` (
  `Review_ID` int NOT NULL AUTO_INCREMENT,
  `Ride_ID` int NOT NULL,
  `Reviewer_ID` int NOT NULL,
  `Driver_ID` int NOT NULL,
  `Rating_Stars` int DEFAULT NULL,
  `Comments` text,
  `Review_Date` datetime DEFAULT NULL,
  PRIMARY KEY (`Review_ID`),
  KEY `fk_reviews_ride` (`Ride_ID`),
  KEY `fk_reviews_reviewer` (`Reviewer_ID`),
  KEY `fk_reviews_driver` (`Driver_ID`),
  CONSTRAINT `fk_reviews_driver` FOREIGN KEY (`Driver_ID`) REFERENCES `Drivers` (`User_ID`),
  CONSTRAINT `fk_reviews_reviewer` FOREIGN KEY (`Reviewer_ID`) REFERENCES `Users` (`User_ID`),
  CONSTRAINT `fk_reviews_ride` FOREIGN KEY (`Ride_ID`) REFERENCES `Rides` (`Ride_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Rides`
--

DROP TABLE IF EXISTS `Rides`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Rides` (
  `Ride_ID` int NOT NULL AUTO_INCREMENT,
  `Driver_ID` int NOT NULL,
  `Vehicle_ID` int NOT NULL,
  `Origin` varchar(100) DEFAULT NULL,
  `Destination` varchar(100) DEFAULT NULL,
  `Departure_Date` datetime DEFAULT NULL,
  `Seats_Left` int DEFAULT NULL,
  `Status` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`Ride_ID`),
  KEY `fk_rides_driver` (`Driver_ID`),
  KEY `fk_rides_vehicle` (`Vehicle_ID`),
  CONSTRAINT `fk_rides_driver` FOREIGN KEY (`Driver_ID`) REFERENCES `Drivers` (`User_ID`),
  CONSTRAINT `fk_rides_vehicle` FOREIGN KEY (`Vehicle_ID`) REFERENCES `Vehicles` (`Vehicle_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Saved_Routes`
--

DROP TABLE IF EXISTS `Saved_Routes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Saved_Routes` (
  `Route_ID` int NOT NULL AUTO_INCREMENT,
  `User_ID` int NOT NULL,
  `Start_Location` varchar(100) DEFAULT NULL,
  `End_Location` varchar(100) DEFAULT NULL,
  `Frequency` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`Route_ID`),
  UNIQUE KEY `unique_user_route` (`User_ID`,`Start_Location`,`End_Location`),
  CONSTRAINT `fk_saved_routes_user` FOREIGN KEY (`User_ID`) REFERENCES `Users` (`User_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Users`
--

DROP TABLE IF EXISTS `Users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Users` (
  `User_ID` int NOT NULL AUTO_INCREMENT,
  `SJSU_ID` varchar(20) DEFAULT NULL,
  `First_Name` varchar(50) DEFAULT NULL,
  `Last_Name` varchar(50) DEFAULT NULL,
  `Email` varchar(100) DEFAULT NULL,
  `Gender` varchar(10) DEFAULT NULL,
  `Password_Hash` varchar(255) DEFAULT NULL,
  `Account_Status` varchar(20) NOT NULL DEFAULT 'active',
  PRIMARY KEY (`User_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Vehicles`
--

DROP TABLE IF EXISTS `Vehicles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Vehicles` (
  `Vehicle_ID` int NOT NULL AUTO_INCREMENT,
  `Driver_ID` int NOT NULL,
  `License_Plate` varchar(20) DEFAULT NULL,
  `Make` varchar(50) DEFAULT NULL,
  `Model` varchar(50) DEFAULT NULL,
  `Color` varchar(20) DEFAULT NULL,
  `Total_Seats` int DEFAULT NULL,
  `Insurance_Num` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`Vehicle_ID`),
  KEY `fk_vehicles_driver` (`Driver_ID`),
  CONSTRAINT `fk_vehicles_driver` FOREIGN KEY (`Driver_ID`) REFERENCES `Drivers` (`User_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-04 22:16:06
