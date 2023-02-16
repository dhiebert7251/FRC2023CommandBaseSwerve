// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;


import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import static frc.robot.Constants.DriveConstants.*;

import java.util.Optional;

import com.kauailabs.navx.frc.AHRS;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.PhysicalConstants;
import frc.robot.lib.PhotonCameraWrapper;
import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import org.photonvision.EstimatedRobotPose;

public class Drivetrain extends SubsystemBase {
  // Robot swerve modules
    /**
   * Constructs a SwerveModule.
   *
   * @param driveMotorChannel The channel of the drive motor SparkMax
   * @param turningMotorChannel The channel of the turning motor VictorSPX
   * @param driveMotorReversed Is the drive motor reversed
   * @param turningMotorReversed Is the turning motor reversed
   * @param turningEncoderChannel ANA port of MA3 turning encoder
   * @param turningEncoderOffset calculated value for offset
   * @param turningEncoderReversed is the turning encoder reversed
   */
  private final SwerveModule m_frontLeft =
      new SwerveModule(
          kFrontLeftDriveMotorPort,
          kFrontLeftTurningMotorPort,
          kFrontLeftDriveMotorReversed,
          kFrontLeftTurningMotorReversed,
          kFrontLeftTurningEncoderPorts,
          kFrontLeftTurningEncoderOffset
          );

  private final SwerveModule m_rearLeft =
      new SwerveModule(
          kRearLeftDriveMotorPort,
          kRearLeftTurningMotorPort,
          kRearLeftDriveMotorReversed,
          kRearLeftTurningMotorReversed,
          kRearLeftTurningEncoderPorts,
          kRearLeftTurningEncoderOffset
          );

  private final SwerveModule m_frontRight =
      new SwerveModule(
          kFrontRightDriveMotorPort,
          kFrontRightTurningMotorPort,
          kFrontRightDriveMotorReversed,
          kFrontRightTurningMotorReversed,
          kFrontRightTurningEncoderPorts,
          kFrontRightTurningEncoderOffset
          );

  private final SwerveModule m_rearRight =
      new SwerveModule(
          kRearRightDriveMotorPort,
          kRearRightTurningMotorPort,
          kRearRightDriveMotorReversed,
          kRearRightTurningMotorReversed,
          kRearRightTurningEncoderPorts,
          kRearRightTurningEncoderOffset
          );

  // The navX MXP gyro sensor
  private final AHRS m_gyro = new AHRS();

  private final SwerveDriveKinematics m_kinematics =
      PhysicalConstants.kDriveKinematics;

  //vision stuff
  public PhotonCameraWrapper pcw;



/*
* Here we use SwerveDrivePoseEstimator so that we can fuse odometry
* readings. The
* numbers used below are robot specific, and should be tuned.
*/
  private final SwerveDrivePoseEstimator m_poseEstimator =
      new SwerveDrivePoseEstimator(m_kinematics, 
                                   getRotation2d(), 
                                   new SwerveModulePosition[] {
                                      m_frontLeft.getPosition(),
                                      m_frontRight.getPosition(),
                                      m_rearLeft.getPosition(),
                                      m_rearRight.getPosition()}, 
                                    getPose());


  // Odometry class for tracking robot pose
  
  SwerveDriveOdometry m_odometry =
      new SwerveDriveOdometry(
          PhysicalConstants.kDriveKinematics,
          m_gyro.getRotation2d(),
          new SwerveModulePosition[] {
            m_frontLeft.getPosition(),
            m_frontRight.getPosition(),
            m_rearLeft.getPosition(),
            m_rearRight.getPosition()
          });



  /** Creates a new DriveSubsystem. */
  public Drivetrain() {
    pcw = new PhotonCameraWrapper();

    //pause gyro reset 1 sec to avoid interferring with gyro calibration
    new Thread(() -> {
      try {
        Thread.sleep(1000);
        zeroHeading();
      } catch (Exception e) {
      }
    }).start();
  }

  @Override
  public void periodic() {
    // Update the odometry in the periodic block
    updateOdometry();

    //vision





    // Update sensor readings
    SmartDashboard.putNumber("Gyro Angle",m_gyro.getAngle());
    SmartDashboard.putNumber("Gyro Rate",m_gyro.getRate());
    SmartDashboard.putNumber("Robot Heading", getHeading());
    SmartDashboard.putString("Robot Location", getPose().getTranslation().toString());
    //new additions - useful?
    SmartDashboard.putNumber("Robot X", m_odometry.getPoseMeters().getX());
    SmartDashboard.putNumber("Robot Y", m_odometry.getPoseMeters().getY());
    SmartDashboard.putNumber("Robot Rotation",
        m_odometry.getPoseMeters().getRotation().getDegrees());

  }

  /**
   * Returns the currently-estimated pose of the robot.
   *
   * @return The pose.
   */
  public Pose2d getPose() {
    return m_odometry.getPoseMeters();
  }

  /**
   * Resets the odometry to the specified pose.
   *
   * @param pose The pose to which to set the odometry.
   */
  public void resetOdometry(Pose2d pose) {
    m_odometry.resetPosition(
        m_gyro.getRotation2d(),
        new SwerveModulePosition[] {
          m_frontLeft.getPosition(),
          m_frontRight.getPosition(),
          m_rearLeft.getPosition(),
          m_rearRight.getPosition()
        },
        pose);
  }

  /**
   * Method to drive the robot using joystick info.
   *
   * @param xSpeed Speed of the robot in the x direction (forward).
   * @param ySpeed Speed of the robot in the y direction (sideways).
   * @param rot Angular rate of the robot.
   * @param fieldRelative Whether the provided x and y speeds are relative to the field.
   */

  // Is this section necessary with the DriveWithJoystics command? 
  // Used in autonomous example

  public void drive(double xSpeed, double ySpeed, double rot, boolean fieldRelative) {
    var swerveModuleStates =
          PhysicalConstants.kDriveKinematics.toSwerveModuleStates(
            fieldRelative
                ? ChassisSpeeds.fromFieldRelativeSpeeds(xSpeed, ySpeed, rot, m_gyro.getRotation2d())
                : new ChassisSpeeds(xSpeed, ySpeed, rot));
    SwerveDriveKinematics.desaturateWheelSpeeds(
        swerveModuleStates, kMaxSpeedMetersPerSecond);
    m_frontLeft.setDesiredState(swerveModuleStates[0]);
    m_frontRight.setDesiredState(swerveModuleStates[1]);
    m_rearLeft.setDesiredState(swerveModuleStates[2]);
    m_rearRight.setDesiredState(swerveModuleStates[3]);
  }
  
  /**
   * Sets the swerve ModuleStates.
   *
   * @param desiredStates The desired SwerveModule states.
   */
  public void setModuleStates(SwerveModuleState[] desiredStates) {
    SwerveDriveKinematics.desaturateWheelSpeeds(
        desiredStates, kMaxSpeedMetersPerSecond);
    m_frontLeft.setDesiredState(desiredStates[0]);
    m_frontRight.setDesiredState(desiredStates[1]);
    m_rearLeft.setDesiredState(desiredStates[2]);
    m_rearRight.setDesiredState(desiredStates[3]);
  }

  /** Resets the drive encoders to currently read a position of 0. */
  public void resetEncoders() {
    m_frontLeft.resetEncoders();
    m_rearLeft.resetEncoders();
    m_frontRight.resetEncoders();
    m_rearRight.resetEncoders();
  }

  /** Zeroes the heading of the robot. */
  public void zeroHeading() {
    m_gyro.reset();
  }

  /**
   * Returns the heading of the robot.
   *
   * @return the robot's heading in degrees, from -180 to 180
   */
  public double getHeading() {
    return Math.IEEEremainder(m_gyro.getAngle(),360);
  }

  public Rotation2d getRotation2d(){
    return Rotation2d.fromDegrees(getHeading());
  }

  /**
   * Returns the turn rate of the robot.
   *
   * @return The turn rate of the robot, in degrees per second
   */
  public double getTurnRate() {
    return m_gyro.getRate() * (kGyroReversed ? -1.0 : 1.0);
  }


  public void stopModules() {
    m_frontLeft.stop();
    m_rearLeft.stop();
    m_frontRight.stop();
    m_rearRight.stop();
  }

  public void wheelsIn() {
    m_frontLeft.setDesiredState(new SwerveModuleState(2, Rotation2d.fromDegrees(45)));
    m_rearLeft.setDesiredState(new SwerveModuleState(2, Rotation2d.fromDegrees(135)));
    m_frontRight.setDesiredState(new SwerveModuleState(2, Rotation2d.fromDegrees(-45)));
    m_rearRight.setDesiredState(new SwerveModuleState(2, Rotation2d.fromDegrees(-135)));
    this.stopModules();
    //TODO: set drive controllers to brake?
  }

/** Updates the field-relative position. */
    public void updateOdometry() {
        m_poseEstimator.update(
                m_gyro.getRotation2d(), 
                new SwerveModulePosition[] {
                  m_frontLeft.getPosition(),
                  m_frontRight.getPosition(),
                  m_rearLeft.getPosition(),
                  m_rearRight.getPosition()
        });

        Optional<EstimatedRobotPose> result =
                pcw.getEstimatedGlobalPose(m_poseEstimator.getEstimatedPosition());

        if (result.isPresent()) {
            EstimatedRobotPose camPose = result.get();
            m_poseEstimator.addVisionMeasurement(
                    camPose.estimatedPose.toPose2d(), camPose.timestampSeconds);
            //m_fieldSim.getObject("Cam Est Pos").setPose(camPose.estimatedPose.toPose2d());
        } else {
            // move it way off the screen to make it disappear
            //m_fieldSim.getObject("Cam Est Pos").setPose(new Pose2d(-100, -100, new Rotation2d()));
        }
        // do i need the simulations?
        //m_fieldSim.getObject("Actual Pos").setPose(m_drivetrainSimulator.getPose());
        //m_fieldSim.setRobotPose(m_poseEstimator.getEstimatedPosition());
    }
}

