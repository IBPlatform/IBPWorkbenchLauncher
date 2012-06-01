This directory should contain the crop database scripts
for central and local databases.
The files should be grouped into folders:
	central
	local
and inside the central/local folders, you should put scripts on
the following folders:
	rice
	chickpea
	cowpea
	maize
	wheat

Each folder should contain the scripts for that crop database
and each script should be prefixed by anything (a number is a good choice)
such that the execution order is the same as the filename order of the scripts.

Example:
central
	rice
		01_ibdbv1_central_iris_innodb_201202_structure.sql
		02_ibdbv1_central_iris_innodb_201202_data_A-I.sql
		03_ibdbv1_central_iris_innodb_201202_data_L-N.sql
		04_ibdbv1_central_iris_innodb_201202_data_O-V.sql
local
	rice