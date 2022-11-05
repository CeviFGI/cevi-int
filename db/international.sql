-- phpMyAdmin SQL Dump
-- version 5.2.0
-- https://www.phpmyadmin.net/
--
-- Host: mysql
-- Generation Time: Nov 05, 2022 at 11:01 PM
-- Server version: 8.0.31
-- PHP Version: 8.0.19

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `international`
--

-- --------------------------------------------------------

--
-- Table structure for table `EventEntity`
--

CREATE TABLE `EventEntity` (
  `id` bigint NOT NULL,
  `date` varchar(255) DEFAULT NULL,
  `description` text,
  `location` varchar(255) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `EventEntity`
--

INSERT INTO `EventEntity` (`id`, `date`, `description`, `location`, `title`) VALUES
(1, '15. - 24. Oktober 2022', 'Unterstützt durch Horyzon. Kosten: 900 - 1\'200 USD je nach Unterkunft + Reise. Das Programm ist eine internationale Solidaritäts- und Schutzaktion für die palästinensischen Bauern und Familien, um sie bei der Ernte ihrer Olivenbäume zu unterstützen, die von Angriffen, Entwurzlung, Beschlagnahmungen und Einschränkungen bedroht sind. Neben der Olivenernte umfasst das Programm auch Führungen in verschiedenen Städten, palästinensischer Kultur und einführernde Vorträge zu Konfliktthemen.<br><a href=\"https://www.jai-pal.org/en/campaigns/olive-tree-campaign/olive-picking-program/picking2022-inv\">Weitere Informationen</a>', 'Palästina', 'Olivenernte'),
(2, '20. - 23. Oktober 2022', 'Fest zu den christlichen Wurzeln des Cevi. Kosten: 95 Euro Festival + Reise + Unterkunft.<br><a href=\"https://www.ymca-unify.eu/european/unify_2022\">Weitere Informationen</a>, <a href=\"https://mcusercontent.com/4164786c200962ea4be64ffd8/files/20dfc6b4-c4f8-19fe-6006-d42a99a47a60/Program_Unify_2022.pdf\">Programm</a>', 'Wien', 'European Unify Conference'),
(3, 'Mai 2023', '', 'Antwerp', 'YMCA General Assembly'),
(4, '28. Juni - 02. Juli 2023', 'Es werden ca. 500 Personen erwartet. Detailinformationen folgen Ende September/Anfang Oktober.', 'Berlin', 'YE 50th anniversary');

-- --------------------------------------------------------

--
-- Table structure for table `ExchangeEntity`
--

CREATE TABLE `ExchangeEntity` (
  `id` bigint NOT NULL,
  `description` text,
  `organization` varchar(255) DEFAULT NULL,
  `organizationLink` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `ExchangeEntity`
--

INSERT INTO `ExchangeEntity` (`id`, `description`, `organization`, `organizationLink`) VALUES
(10, 'Palästina', 'Olivenernte', '15. - 24. Oktober 2022'),
(11, 'Wien', 'European Unify Conference', '20. - 23. Oktober 2022'),
(12, 'Berlin', 'YE 50th anniversary', '28. Juni - 02. Juli 2023');

-- --------------------------------------------------------

--
-- Table structure for table `hibernate_sequence`
--

CREATE TABLE `hibernate_sequence` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `hibernate_sequence`
--

INSERT INTO `hibernate_sequence` (`next_val`) VALUES
(13);

-- --------------------------------------------------------

--
-- Table structure for table `VoluntaryServiceEntity`
--

CREATE TABLE `VoluntaryServiceEntity` (
  `id` bigint NOT NULL,
  `description` text,
  `location` varchar(255) DEFAULT NULL,
  `organization` varchar(255) DEFAULT NULL,
  `organizationLink` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `VoluntaryServiceEntity`
--

INSERT INTO `VoluntaryServiceEntity` (`id`, `description`, `location`, `organization`, `organizationLink`) VALUES
(5, 'Dauer 2 Wochen oder 3-6 Monate. Siehe <a href=\"https://horyzon.ch/de/spenden-und-unterstuetzen/einsaetze-weltweit/volontariat/\">Weitere Informationen</a>', 'Kolumbien, Palästina', 'Horyzon', 'https://horyzon.ch/'),
(6, 'Dauer 2 Wochen oder 3-6 Monate. Siehe <a href=\"https://horyzon.ch/de/spenden-und-unterstuetzen/einsaetze-weltweit/volontariat/\">Weitere Informationen</a>', 'Kolumbien, Palästina', 'Volunteers for Europe', 'http://vfe.cvjm.de/'),
(7, 'Dauer 2 Wochen oder 3-6 Monate. Siehe <a href=\"https://horyzon.ch/de/spenden-und-unterstuetzen/einsaetze-weltweit/volontariat/\">Weitere Informationen</a>', 'Kolumbien, Palästina', 'Horyzon', 'https://horyzon.ch/'),
(8, 'Dauer 2 Wochen oder 3-6 Monate. Siehe <a href=\"https://horyzon.ch/de/spenden-und-unterstuetzen/einsaetze-weltweit/volontariat/\">Weitere Informationen</a>', 'Kolumbien, Palästina', 'Horyzon', 'https://horyzon.ch/'),
(9, 'Dauer 2 Wochen oder 3-6 Monate. Siehe <a href=\"https://horyzon.ch/de/spenden-und-unterstuetzen/einsaetze-weltweit/volontariat/\">Weitere Informationen</a>', 'Kolumbien, Palästina', 'Horyzon', 'https://horyzon.ch/');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `EventEntity`
--
ALTER TABLE `EventEntity`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `ExchangeEntity`
--
ALTER TABLE `ExchangeEntity`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `VoluntaryServiceEntity`
--
ALTER TABLE `VoluntaryServiceEntity`
  ADD PRIMARY KEY (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
