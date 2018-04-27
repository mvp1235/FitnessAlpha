# FitnessAlpha

This application utilizes the built-in accelerometer sensor in Android devices to compute workout information such as footsteps, calories burned, distanced traveled, etc.
During active workouts in landscape mode, the application displays a real time update of calories burned and step counts on a graph plotted using MPAndroidChart library, as well as the rate of maximum, minimum, and average minutes per mile.
A Content Provider is used to keep a record of all workouts performed, and the data is saved locally to the device's internal storage. Remote services and threads are utilized to keep the application running in the background, even in the case of the screen being turned off.

# In-App Interface

### Landscape Mode During Workout
<img src="https://user-images.githubusercontent.com/15053859/39386779-138fe19e-4a2b-11e8-89c5-54b176f5ca53.jpg" width="300" height="500">

### Portrait Mode During Workout
<img src="https://user-images.githubusercontent.com/15053859/39386780-13a9152e-4a2b-11e8-8ede-28a00300145b.jpg" width="300" height="500">

### Profile Page
<img src="https://user-images.githubusercontent.com/15053859/39386781-13c25ce6-4a2b-11e8-9bfb-34f5041135de.jpg" width="300" height="500">

### Edit Profile Page
<img src="https://user-images.githubusercontent.com/15053859/39386778-137673a8-4a2b-11e8-8527-d48bfa8b9eb6.jpg" width="300" height="500">
