# Backend sample

This is a functional specification for a hypothetical front-end application 
that could be realized on top of the Backend sample app.

## Log in

- Log in
- Register new users
- Home page
- Display and update current user's profile
- Change current user's password
- Delete current user's account

### Log in (/login)

- EditForm
	- Email or username (required)
	- Password (required)
	- [Log in](#/home)
	- [Register](#/register)

### Register (/register)

- EditForm
	- Email (required)
	- Username (required)
	- Password (required)
	- [Register](#/home)
	- [Log in](#/login)

### Home (/home)

- ReadOnlyForm
	- [Profile](#/profile)
	- [Log out](#/login)

### Display profile (/profile)

- ReadOnlyForm
	- Email: [leela@gmail.com](mailto:leela@gmail.com)
	- Username: leela
	- Full name: Turanga Leela
	- [Edit](#/profile/edit)
	- [Change password](#/change-password)
	- [Delete account](#/login)
		After a confirmation deletes all data of the user (i.e. the projects)
		and then deletes the user record itself.
	- [Home](#/home)

### Edit profile (/profile/edit)

- EditForm
	- Email (required): leela@gmail.com
	- Username (required): leela
	- Full name (required): Turanga Leela
	- [Save](#/profile)
	- [Cancel](#/profile)

### Change password (/change-password)

- ReadOnlyForm
	- Password (required)
	- [Save](#/profile)
	- [Cancel](#/profile)

## Users

- List system users

### List users (/users)

- ReadOnlyTable Users
	- Full name
		- Turanga Leela
		- Philip J. Fry
		- John A. Zoidberg
	- Username
		- leela
		- fry
		- zoidberg
	- Email
		- [leela@gmail.com](mailto:leela@gmail.com)
		- [fry@gmail.com](mailto:fry@gmail.com)
		- [doctor@planet-express.com](mailto:doctor@planet-express.com)

## Projects

- Maintain user's projects

### List projects (/projects)

- ReadOnlyTable Projects
	- Name
		- Find another cyclop
		- Save Nibbler
		- Kill the whale
	- Code
		- find-a-cyclop
		- save-nibbler
		- kill-the-whale
	- Visibility
		- private
		- public
		- public
