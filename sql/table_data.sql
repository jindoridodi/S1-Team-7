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
-- Dumping data for table `Bookings`
--

LOCK TABLES `Bookings` WRITE;
/*!40000 ALTER TABLE `Bookings` DISABLE KEYS */;
INSERT INTO `Bookings` VALUES (1,11,1,'San Jose, CA','SJSU Campus','2026-05-20 07:30:00',1,'2026-05-15 10:00:00','confirmed'),(2,12,1,'San Jose, CA','SJSU Campus','2026-05-20 07:30:00',1,'2026-05-15 10:05:00','confirmed'),(3,13,2,'Santa Clara, CA','SJSU Campus','2026-05-20 08:00:00',1,'2026-05-15 11:00:00','confirmed'),(4,14,3,'Fremont, CA','SJSU Campus','2026-05-20 08:30:00',1,'2026-05-15 12:00:00','confirmed'),(5,15,4,'San Jose, CA','SJSU Campus','2026-05-20 09:00:00',1,'2026-05-15 13:00:00','confirmed'),(6,16,5,'Sunnyvale, CA','SJSU Campus','2026-05-21 08:00:00',1,'2026-05-15 14:00:00','confirmed'),(7,17,7,'SJSU Campus','Fremont, CA','2026-05-19 17:30:00',1,'2026-05-14 09:00:00','completed'),(8,18,8,'Campbell, CA','SJSU Campus','2026-05-19 08:00:00',1,'2026-05-14 10:00:00','completed'),(9,19,9,'SJSU Campus','Milpitas, CA','2026-05-18 17:00:00',1,'2026-05-13 08:00:00','completed'),(10,20,10,'Los Gatos, CA','SJSU Campus','2026-05-18 07:45:00',1,'2026-05-13 09:00:00','completed');
/*!40000 ALTER TABLE `Bookings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `Drivers`
--

LOCK TABLES `Drivers` WRITE;
/*!40000 ALTER TABLE `Drivers` DISABLE KEYS */;
INSERT INTO `Drivers` VALUES (1,'CA-D1000001','verified',4.8),(2,'CA-D1000002','verified',4.6),(3,'CA-D1000003','verified',4.9),(4,'CA-D1000004','verified',4.5),(5,'CA-D1000005','pending',4.2),(6,'CA-D1000006','verified',4.7),(7,'CA-D1000007','verified',4.3),(8,'CA-D1000008','verified',4.8),(9,'CA-D1000009','pending',3.9),(10,'CA-D1000010','verified',4.6);
/*!40000 ALTER TABLE `Drivers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `Logs`
--

LOCK TABLES `Logs` WRITE;
/*!40000 ALTER TABLE `Logs` DISABLE KEYS */;
INSERT INTO `Logs` VALUES (1,1,NULL,NULL,'ACCOUNT_CREATED','2026-05-01 09:00:00','User Aiden Nguyen created an account.'),(2,2,NULL,NULL,'ACCOUNT_CREATED','2026-05-01 09:05:00','User Sofia Patel created an account.'),(3,1,1,NULL,'RIDE_CREATED','2026-05-10 08:00:00','Driver Aiden created ride #1 to SJSU Campus.'),(4,2,2,NULL,'RIDE_CREATED','2026-05-10 08:30:00','Driver Sofia created ride #2 to SJSU Campus.'),(5,11,NULL,1,'BOOKING_CREATED','2026-05-15 10:00:00','Ryan booked a seat on ride #1.'),(6,12,NULL,2,'BOOKING_CREATED','2026-05-15 10:05:00','Zoe booked a seat on ride #1.'),(7,17,NULL,7,'BOOKING_CANCELLED','2026-05-14 08:30:00','Kevin cancelled booking #7.'),(8,7,7,NULL,'RIDE_CANCELLED','2026-05-19 07:00:00','Driver Carlos cancelled ride #7.'),(9,1,NULL,NULL,'LOGIN','2026-05-15 07:55:00','Aiden logged in.'),(10,11,NULL,NULL,'LOGIN','2026-05-15 09:58:00','Ryan logged in.');
/*!40000 ALTER TABLE `Logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `Notifications`
--

LOCK TABLES `Notifications` WRITE;
/*!40000 ALTER TABLE `Notifications` DISABLE KEYS */;
INSERT INTO `Notifications` VALUES (1,11,'Your booking for the ride to SJSU Campus on May 20 is confirmed!','2026-05-15 10:01:00','read'),(2,12,'Your booking for the ride to SJSU Campus on May 20 is confirmed!','2026-05-15 10:06:00','read'),(3,13,'Your booking for the ride to SJSU Campus on May 20 is confirmed!','2026-05-15 11:01:00','unread'),(4,14,'Your booking for the ride to SJSU Campus on May 20 is confirmed!','2026-05-15 12:01:00','unread'),(5,15,'Your booking for the ride to SJSU Campus on May 20 is confirmed!','2026-05-15 13:01:00','read'),(6,16,'Your booking for the ride to SJSU Campus on May 21 is confirmed!','2026-05-15 14:01:00','read'),(7,1,'New passenger booked your ride to SJSU Campus on May 20.','2026-05-15 10:02:00','read'),(8,2,'New passenger booked your ride to SJSU Campus on May 20.','2026-05-15 11:02:00','read'),(9,17,'Your ride to Fremont on May 19 is complete. Please leave a review!','2026-05-19 18:00:00','read'),(10,18,'Your ride to SJSU Campus on May 19 is complete. Please leave a review!','2026-05-19 18:00:00','unread');
/*!40000 ALTER TABLE `Notifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `Passengers`
--

LOCK TABLES `Passengers` WRITE;
/*!40000 ALTER TABLE `Passengers` DISABLE KEYS */;
INSERT INTO `Passengers` VALUES (11,3),(12,7),(13,2),(14,10),(15,5),(16,8),(17,1),(18,4),(19,6),(20,9);
/*!40000 ALTER TABLE `Passengers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `Reviews`
--

LOCK TABLES `Reviews` WRITE;
/*!40000 ALTER TABLE `Reviews` DISABLE KEYS */;
INSERT INTO `Reviews` VALUES (1,7,17,7,5,'Super smooth ride, very professional driver!','2026-05-19 20:00:00'),(2,7,12,7,4,'Good ride, a bit of traffic but driver handled it well.','2026-05-19 20:30:00'),(3,8,18,8,5,'Arrived on time, friendly and safe driver.','2026-05-19 18:30:00'),(4,8,13,8,5,'Best carpool experience so far at SJSU!','2026-05-19 21:30:00'),(5,9,19,9,3,'Driver was slightly late but the ride was okay.','2026-05-18 19:00:00'),(6,9,14,9,4,'Pleasant ride, good conversation.','2026-05-18 19:30:00'),(7,10,20,10,5,'Excellent driver, very punctual. Highly recommend!','2026-05-18 20:00:00'),(8,10,11,10,5,'Great experience, clean car and smooth drive.','2026-05-18 20:30:00'),(9,7,15,7,4,'Comfortable ride, driver was friendly.','2026-05-19 21:00:00'),(10,8,16,8,4,'Nice car, would book again.','2026-05-19 19:00:00');
/*!40000 ALTER TABLE `Reviews` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `Rides`
--

LOCK TABLES `Rides` WRITE;
/*!40000 ALTER TABLE `Rides` DISABLE KEYS */;
INSERT INTO `Rides` VALUES (1,1,1,'San Jose, CA','SJSU Campus','2026-05-20 07:30:00',2,'scheduled'),(2,2,2,'Santa Clara, CA','SJSU Campus','2026-05-20 08:00:00',1,'scheduled'),(3,3,3,'Fremont, CA','SJSU Campus','2026-05-20 08:30:00',3,'scheduled'),(4,4,4,'San Jose, CA','SJSU Campus','2026-05-20 09:00:00',2,'scheduled'),(5,5,5,'Sunnyvale, CA','SJSU Campus','2026-05-21 08:00:00',1,'scheduled'),(6,6,6,'Milpitas, CA','SJSU Campus','2026-05-21 09:00:00',3,'scheduled'),(7,7,7,'SJSU Campus','Fremont, CA','2026-05-19 17:30:00',0,'completed'),(8,8,8,'Campbell, CA','SJSU Campus','2026-05-19 08:00:00',0,'completed'),(9,9,9,'SJSU Campus','Milpitas, CA','2026-05-18 17:00:00',0,'completed'),(10,10,10,'Los Gatos, CA','SJSU Campus','2026-05-18 07:45:00',0,'completed');
/*!40000 ALTER TABLE `Rides` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `Saved_Routes`
--

LOCK TABLES `Saved_Routes` WRITE;
/*!40000 ALTER TABLE `Saved_Routes` DISABLE KEYS */;
INSERT INTO `Saved_Routes` VALUES (1,11,'San Jose, CA','SJSU Campus','daily'),(2,12,'Sunnyvale, CA','SJSU Campus','weekly'),(3,13,'Santa Clara, CA','SJSU Campus','daily'),(4,14,'Fremont, CA','SJSU Campus','daily'),(5,15,'Milpitas, CA','SJSU Campus','weekly'),(6,16,'Campbell, CA','SJSU Campus','daily'),(7,17,'Los Gatos, CA','SJSU Campus','weekly'),(8,18,'San Jose, CA','SJSU Campus','daily'),(9,19,'Cupertino, CA','SJSU Campus','weekly'),(10,20,'Morgan Hill, CA','SJSU Campus','daily');
/*!40000 ALTER TABLE `Saved_Routes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `Users`
--

LOCK TABLES `Users` WRITE;
/*!40000 ALTER TABLE `Users` DISABLE KEYS */;
INSERT INTO `Users` VALUES (1,'012345601','Aiden','Nguyen','aiden.nguyen@sjsu.edu','Male','1d95a4c6d681ede5b18c89b21ceb46bfea7b8e4d8f824107615a2ee297493710','active'),(2,'012345602','Sofia','Patel','sofia.patel@sjsu.edu','Female','1d95a4c6d681ede5b18c89b21ceb46bfea7b8e4d8f824107615a2ee297493710','active'),(3,'012345603','Marcus','Johnson','marcus.johnson@sjsu.edu','Male','1d95a4c6d681ede5b18c89b21ceb46bfea7b8e4d8f824107615a2ee297493710','active'),(4,'012345604','Priya','Sharma','priya.sharma@sjsu.edu','Female','1d95a4c6d681ede5b18c89b21ceb46bfea7b8e4d8f824107615a2ee297493710','active'),(5,'012345605','Ethan','Kim','ethan.kim@sjsu.edu','Male','1d95a4c6d681ede5b18c89b21ceb46bfea7b8e4d8f824107615a2ee297493710','active'),(6,'012345606','Leila','Hassan','leila.hassan@sjsu.edu','Female','1d95a4c6d681ede5b18c89b21ceb46bfea7b8e4d8f824107615a2ee297493710','active'),(7,'012345607','Carlos','Rivera','carlos.rivera@sjsu.edu','Male','1d95a4c6d681ede5b18c89b21ceb46bfea7b8e4d8f824107615a2ee297493710','active'),(8,'012345608','Mei','Chen','mei.chen@sjsu.edu','Female','1d95a4c6d681ede5b18c89b21ceb46bfea7b8e4d8f824107615a2ee297493710','active'),(9,'012345609','Jordan','Taylor','jordan.taylor@sjsu.edu','Male','1d95a4c6d681ede5b18c89b21ceb46bfea7b8e4d8f824107615a2ee297493710','active'),(10,'012345610','Anaya','Williams','anaya.williams@sjsu.edu','Female','1d95a4c6d681ede5b18c89b21ceb46bfea7b8e4d8f824107615a2ee297493710','active'),(11,'012345611','Ryan','Okafor','ryan.okafor@sjsu.edu','Male','1d95a4c6d681ede5b18c89b21ceb46bfea7b8e4d8f824107615a2ee297493710','active'),(12,'012345612','Zoe','Martinez','zoe.martinez@sjsu.edu','Female','1d95a4c6d681ede5b18c89b21ceb46bfea7b8e4d8f824107615a2ee297493710','active'),(13,'012345613','Dev','Kapoor','dev.kapoor@sjsu.edu','Male','1d95a4c6d681ede5b18c89b21ceb46bfea7b8e4d8f824107615a2ee297493710','active'),(14,'012345614','Nina','Park','nina.park@sjsu.edu','Female','1d95a4c6d681ede5b18c89b21ceb46bfea7b8e4d8f824107615a2ee297493710','active'),(15,'012345615','Lucas','Brown','lucas.brown@sjsu.edu','Male','1d95a4c6d681ede5b18c89b21ceb46bfea7b8e4d8f824107615a2ee297493710','active'),(16,'012345616','Amara','Diallo','amara.diallo@sjsu.edu','Female','1d95a4c6d681ede5b18c89b21ceb46bfea7b8e4d8f824107615a2ee297493710','active'),(17,'012345617','Kevin','Lee','kevin.lee@sjsu.edu','Male','1d95a4c6d681ede5b18c89b21ceb46bfea7b8e4d8f824107615a2ee297493710','active'),(18,'012345618','Sara','Gonzalez','sara.gonzalez@sjsu.edu','Female','1d95a4c6d681ede5b18c89b21ceb46bfea7b8e4d8f824107615a2ee297493710','active'),(19,'012345619','Omar','Farouk','omar.farouk@sjsu.edu','Male','1d95a4c6d681ede5b18c89b21ceb46bfea7b8e4d8f824107615a2ee297493710','active'),(20,'012345620','Hannah','White','hannah.white@sjsu.edu','Female','1d95a4c6d681ede5b18c89b21ceb46bfea7b8e4d8f824107615a2ee297493710','active');
/*!40000 ALTER TABLE `Users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `Vehicles`
--

LOCK TABLES `Vehicles` WRITE;
/*!40000 ALTER TABLE `Vehicles` DISABLE KEYS */;
INSERT INTO `Vehicles` VALUES (1,1,'7ABC123','Toyota','Camry','Silver',4,'active'),(2,2,'8DEF456','Honda','Civic','Blue',4,'active'),(3,3,'9GHI789','Tesla','Model 3','White',4,'active'),(4,4,'2JKL012','Chevrolet','Malibu','Gray',4,'active'),(5,5,'3MNO345','Ford','Mustang','Red',4,'active'),(6,6,'4PQR678','Hyundai','Elantra','Black',4,'active'),(7,7,'5STU901','Kia','Forte','White',4,'active'),(8,8,'6VWX234','Nissan','Altima','Blue',5,'active'),(9,9,'1YZA567','Mazda','Mazda3','Red',4,'active'),(10,10,'9BCD890','Subaru','Impreza','Green',4,'active');
/*!40000 ALTER TABLE `Vehicles` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-15 18:02:35
