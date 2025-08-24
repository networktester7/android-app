# Android Mobile Inventory Application





## Introduction
By 2029, an estimated 1.8 billion people worldwide will be using smartphones, with iOS and Android as the dominant players (Global: Number of smartphone users, 2025). Mobile devices and applications are no longer for personal use only but have become a vital part of the technology ecosystem for most organizations. This project will outline the development of an Android mobile inventory application for a hypothetical medical device company and its sales force. 

## Project Description
This project will develop an Android mobile inventory application that will synchronize an API with the user’s device to request inventory data from either a website or a server data base. Once the application downloads the requested data, it will display current inventory on the device screen. The user will also be able to search for a medical device from within the app.

## Problem Addressing
As private users and organizations become increasingly dependent on smartphones for critical data, mobile applications must keep up with the rapidly advancing technological landscape. Medical sales representatives are often called into emergency surgeries from remote locations. On the road, a rep may not have access to a computer and/or the internet. Checking hospital inventory from a computer is also laborious and time consuming. 

As a sales representative, it is imperative to have hospital inventory data available at a moment’s notice. Inventory data must be searchable, accurate, accessible instantaneously, and secure within the network connection. An Android mobile inventory app will solve this common sales team problem by providing immediate access to live inventory from an Android smartphone or watch, saving time and possibly a life. 

## Platform
The platform is Android OS and will primarily target smartphone devices.

## Front/Back-End Support
Android Studio will be used to develop the app. The backend is a MySQL server that binds to PHP through Apache to handle requests. The PHP server handles the login, add, and subtract requests. Employees can instantly see in real time inventory quantities available. The backend is going to consist of an inventory database API service that distributes live hospital inventory data to sales team devices.


## Functionality
The functionality consists of a display interface that shows the current inventory available at a hospital location, as well as a search function to allow the user to search for a specific medical device (e.g. an 18 mm heart valve).

## Currently functioning features:
1. Login screen authenticates with main app.
2. Users are able to view the database and inventory quantities.
3. Users can add quantites of items.
4. Users can subtract items from the inventory. 

## Design (wireframes)

![469887153-010aef8b-6088-4f5f-948b-305989fe96c1](https://github.com/user-attachments/assets/098d1652-52f4-4435-97c7-9dc5ee234b29)

## Homescreen and Logo

![Screenshot_20250723-124116_Hospital Database](https://github.com/user-attachments/assets/bc4e8b6a-0824-4984-b009-6d33f26b53b0)

## Updates

## **Previous Updates:**

1. Designed and implemented database interface.

2. Added a search function.
3. Added the app to GitHub.
4. Added support for viewing and modifying inventories from multiple locations
5. Added support for removing and addimg quantities of inventory
6. Integrated an interface that refreshes when a quantity is changed

## **Current Updates:**

The Gulf Coast region of FrankLabs, Inc. has three hospitals in the network. I am currently working on developing separate inventory access pages for each hospital (Tampa General, Sarasota Memorial, and Naples Community hospitals), so the employees may access and adjust live inventory data for each destination independently. I am struggling with Android Studio frequently crashing. I am also having issues with my virtual machine and host computer (possible hardware failure).

## **Future Updates:**

1. Improve user experience and interface.

2. Add features to streamline searching. 

3. Streamline modification functionality.

## Changelog

https://github.com/networktester7/android-app/commits/main/


## References


Global: Number of smartphone users 2014-2029. (2025). Statista. 
https://www.statista.com/forecasts/1143723/smartphone-users-in-the-world
![image](https://github.com/user-attachments/assets/5855bd68-75bf-4206-ad73-4023093fdf99)

