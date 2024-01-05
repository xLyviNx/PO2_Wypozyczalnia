-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Sty 02, 2024 at 06:24 PM
-- Wersja serwera: 10.4.32-MariaDB
-- Wersja PHP: 8.0.30

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `wypozyczalnia`
--

-- --------------------------------------------------------

--
-- Struktura tabeli dla tabeli `auta`
--

CREATE TABLE `auta` (
  `id_auta` int(11) NOT NULL,
  `marka` varchar(32) NOT NULL,
  `model` varchar(32) NOT NULL,
  `rok_prod` int(11) NOT NULL,
  `silnik` varchar(32) NOT NULL,
  `zdjecie` varchar(64) DEFAULT NULL,
  `opis` varchar(512) DEFAULT NULL,
  `cenaZaDzien` decimal(10,2) DEFAULT NULL,
  `wiekszeZdjecia` varchar(256) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Struktura tabeli dla tabeli `typy_uzytkownikow`
--

CREATE TABLE `typy_uzytkownikow` (
  `id_typu` int(11) NOT NULL,
  `nazwa_typu` varchar(24) NOT NULL,
  `dodajogloszenia` char(1) NOT NULL,
  `wypozyczauto` char(1) NOT NULL,
  `usunogloszenie` char(1) NOT NULL,
  `manageReservations` tinyint(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `typy_uzytkownikow`
--

INSERT INTO `typy_uzytkownikow` (`id_typu`, `nazwa_typu`, `dodajogloszenia`, `wypozyczauto`, `usunogloszenie`, `manageReservations`) VALUES
(1, 'Uzytkownik', '0', '1', '0', 0),
(2, 'Administrator', '1', '1', '1', 1);

-- --------------------------------------------------------

--
-- Struktura tabeli dla tabeli `uzytkownicy`
--

CREATE TABLE `uzytkownicy` (
  `id_uzytkownika` int(11) NOT NULL,
  `login` varchar(32) NOT NULL,
  `password` varchar(32) NOT NULL,
  `imie` varchar(15) DEFAULT NULL,
  `nazwisko` varchar(32) DEFAULT NULL,
  `data_utworzenia` datetime NOT NULL,
  `numer_telefonu` double NOT NULL,
  `typy_uzytkownikow_id_typu` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `uzytkownicy`
--

INSERT INTO `uzytkownicy` (`id_uzytkownika`, `login`, `password`, `imie`, `nazwisko`, `data_utworzenia`, `numer_telefonu`, `typy_uzytkownikow_id_typu`) VALUES
(1, 'admin', '21232f297a57a5a743894a0e4a801fc3', 'System', 'Administrator', '2024-01-01 20:09:15', 100000000, 2),
(2, 'test1', '5a105e8b9d40e1329780d62ea2265d8a', 'TEST', 'KONTO', '2024-01-02 15:11:35', 110000000, 1),
(3, 'test2', 'ad0234829205b9033196ba818f7a872b', 'TEST', 'KONTO 2', '2024-01-02 16:16:03', 391239193, 1);

-- --------------------------------------------------------

--
-- Struktura tabeli dla tabeli `wypozyczenie`
--

CREATE TABLE `wypozyczenie` (
  `id_wypozyczenia` int(11) NOT NULL,
  `data_wypozyczenia` datetime DEFAULT NULL,
  `uzytkownicy_id_uzytkownika` int(11) NOT NULL,
  `auta_id_auta` int(11) NOT NULL,
  `days` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Indeksy dla zrzutów tabel
--

--
-- Indeksy dla tabeli `auta`
--
ALTER TABLE `auta`
  ADD PRIMARY KEY (`id_auta`);

--
-- Indeksy dla tabeli `typy_uzytkownikow`
--
ALTER TABLE `typy_uzytkownikow`
  ADD PRIMARY KEY (`id_typu`);

--
-- Indeksy dla tabeli `uzytkownicy`
--
ALTER TABLE `uzytkownicy`
  ADD PRIMARY KEY (`id_uzytkownika`),
  ADD KEY `typuzytkownikafk` (`typy_uzytkownikow_id_typu`);

--
-- Indeksy dla tabeli `wypozyczenie`
--
ALTER TABLE `wypozyczenie`
  ADD PRIMARY KEY (`id_wypozyczenia`),
  ADD KEY `wypozyczenie_auta_fk` (`auta_id_auta`),
  ADD KEY `wypozyczenie_uzytkownicy_fk` (`uzytkownicy_id_uzytkownika`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `auta`
--
ALTER TABLE `auta`
  MODIFY `id_auta` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `typy_uzytkownikow`
--
ALTER TABLE `typy_uzytkownikow`
  MODIFY `id_typu` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `uzytkownicy`
--
ALTER TABLE `uzytkownicy`
  MODIFY `id_uzytkownika` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `wypozyczenie`
--
ALTER TABLE `wypozyczenie`
  MODIFY `id_wypozyczenia` int(11) NOT NULL AUTO_INCREMENT;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `uzytkownicy`
--
ALTER TABLE `uzytkownicy`
  ADD CONSTRAINT `typuzytkownikafk` FOREIGN KEY (`typy_uzytkownikow_id_typu`) REFERENCES `typy_uzytkownikow` (`id_typu`);

--
-- Constraints for table `wypozyczenie`
--
ALTER TABLE `wypozyczenie`
  ADD CONSTRAINT `wypozyczenie_auta_fk` FOREIGN KEY (`auta_id_auta`) REFERENCES `auta` (`id_auta`),
  ADD CONSTRAINT `wypozyczenie_uzytkownicy_fk` FOREIGN KEY (`uzytkownicy_id_uzytkownika`) REFERENCES `uzytkownicy` (`id_uzytkownika`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;